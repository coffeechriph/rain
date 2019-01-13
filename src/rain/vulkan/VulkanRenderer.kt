package rain.vulkan

import org.joml.Matrix4f
import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import rain.api.Window
import rain.api.gfx.Drawable
import rain.api.gfx.Renderer
import rain.api.scene.Camera
import rain.assertion
import rain.log
import java.util.*
import kotlin.collections.ArrayList

internal class VulkanRenderer (private val vk: Vk, val window: Window) : Renderer {
    private val pipelines: MutableList<Pipeline> = ArrayList()
    private val physicalDevice: PhysicalDevice = vk.physicalDevice
    private val logicalDevice: LogicalDevice = vk.logicalDevice
    private val renderpass: Renderpass = Renderpass()
    private val swapchain: Swapchain
    private val queueFamilyIndices: QueueFamilyIndices
    private var renderCommandPool: CommandPool = CommandPool()
    private var renderCommandBuffers: Array<CommandPool.CommandBuffer> = emptyArray()
    private var graphicsQueue = Array(2){Queue()}
    private var presentQueue = Array(2){Queue()}
    private var setupQueue = Queue()
    private var surfaceColorFormat = 0

    private lateinit var setupCommandPool: CommandPool
    private lateinit var setupCommandBuffer: CommandPool.CommandBuffer
    private lateinit var postPresentBuffer: CommandPool.CommandBuffer
    private var imageAcquiredSemaphore = Array(0){ Semaphore() }
    private var completeRenderSemaphore = Array(0){ Semaphore() }
    private var drawingFinishedFence = Array(0){ memAllocLong(0)}

    private val pSwapchains = MemoryUtil.memAllocLong(1)
    private val pRenderCompleteSemaphore = MemoryUtil.memAllocLong(1)
    private val pImageIndex = MemoryUtil.memAllocInt(1)

    private val drawOpsQueue = ArrayDeque<Drawable>()

    var swapchainIsDirty = true
    private var frameIndex = 0

    private lateinit var camera: Camera

    init {
        this.queueFamilyIndices = vk.queueFamilyIndices
        this.swapchain = Swapchain()
        this.surfaceColorFormat = vk.surface.format

        for(i in 0 until this.graphicsQueue.size) {
            this.graphicsQueue[i] = Queue()
            this.graphicsQueue[i].create(logicalDevice, queueFamilyIndices.graphicsFamily)

            this.presentQueue[i] = Queue()
            this.presentQueue[i].create(logicalDevice, queueFamilyIndices.presentFamily)
        }

        setupQueue = Queue()
        setupQueue.create(logicalDevice, vk.transferFamilyIndex)
    }

    override fun setActiveCamera(camera: Camera) {
        this.camera = camera
    }

    override fun submitDraw(drawable: Drawable) {
        val vbuf = drawable.vertexBuffer as VulkanVertexBuffer
        if (!vbuf.isValid) {
            assertion("Can't submit a drawable with invalid vertex buffer!")
        }

        val vmat = drawable.material as VulkanMaterial
        if (!vmat.isValid) {
            assertion("Can't submit a drawable with invalid material!")
        }

        if(drawable.indexBuffer != null) {
            if (!(drawable.indexBuffer as VulkanIndexBuffer).isValid) {
                assertion("Cant submit a drawable with invalid index buffer!")
            }
        }

        drawOpsQueue.add(drawable)
    }

    override fun getDepthRange(): Vector2f {
        return Vector2f(0.0f, 20.0f)
    }

    override fun create() {
        renderpass.create(logicalDevice, surfaceColorFormat, findDepthFormat(physicalDevice))
        renderCommandPool.create(logicalDevice, queueFamilyIndices.graphicsFamily)
        postPresentBuffer = renderCommandPool.createCommandBuffer(logicalDevice.device, 1)[0]

        setupCommandPool = CommandPool()
        setupCommandPool.create(logicalDevice, vk.transferFamilyIndex)
        setupCommandBuffer = setupCommandPool.createCommandBuffer(logicalDevice.device, 1)[0]
    }

    fun destroy() {
        vkDeviceWaitIdle(logicalDevice.device)
        vkDestroySwapchainKHR(logicalDevice.device, swapchain.swapchain, null)
        cleanUpResources()
    }

    private fun recreateRenderCommandBuffers() {
        renderCommandBuffers = renderCommandPool.createCommandBuffer(logicalDevice.device, swapchain.framebuffers.size)
    }

    private fun recreateSwapchain(surface: Surface): Boolean {
        if (swapchainIsDirty) {
            log("Swapchain is dirty must recreate!")
            vkDeviceWaitIdle(logicalDevice.device)

            cleanUpResources()
            val capabilities = VkSurfaceCapabilitiesKHR.calloc()
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.device, surface.surface, capabilities)
            val extent2D = VkExtent2D.create()

            if (capabilities.currentExtent().width() != -1) {
                extent2D.width(capabilities.currentExtent().width())
                        .height(capabilities.currentExtent().height())
            }
            else {
                val width = memAllocInt(1)
                val height = memAllocInt(1)
                glfwGetFramebufferSize(window.windowPointer, width, height)
                extent2D.width(width[0])
                        .height(height[0])
            }

            swapchain.create(logicalDevice, physicalDevice, surface, setupCommandBuffer, setupQueue, extent2D)
            renderpass.create(logicalDevice, surface.format, findDepthFormat(physicalDevice))
            swapchain.createFramebuffers(logicalDevice, renderpass, extent2D)
            swapchainIsDirty = false

            completeRenderSemaphore = Array(swapchain.framebuffers.size){ Semaphore() }
            imageAcquiredSemaphore = Array(swapchain.framebuffers.size){ Semaphore() }
            drawingFinishedFence = Array(swapchain.framebuffers.size){ memAllocLong(1)}

            for (i in 0 until completeRenderSemaphore.size) {
                completeRenderSemaphore[i].create(logicalDevice)
                imageAcquiredSemaphore[i].create(logicalDevice)

                val fenceCreateInfo = VkFenceCreateInfo.calloc()
                        .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                        .flags(VK_FENCE_CREATE_SIGNALED_BIT)

                val err = vkCreateFence(logicalDevice.device, fenceCreateInfo, null, drawingFinishedFence[i])
                if (err != VK_SUCCESS) {
                    assertion("Error creating fence " + VulkanResult(err))
                }
            }

            log("Successfully recreated swapchain.")
            return true
        }

        return false
    }

    fun clearPipelines() {
        vkDeviceWaitIdle(logicalDevice.device)
        for (pipeline in pipelines) {
            pipeline.destroy(logicalDevice)
        }
        pipelines.clear()
        vkDeviceWaitIdle(logicalDevice.device)
    }

    private fun cleanUpResources() {
        for (i in 0 until swapchain.framebuffers.size) {
            vkDestroyFramebuffer(logicalDevice.device, swapchain.framebuffers[i], null)
        }
        vkDestroyImage(logicalDevice.device, swapchain.depthImage, null)
        vkDestroyImageView(logicalDevice.device, swapchain.depthImageView, null)

        for (pipeline in pipelines) {
            pipeline.destroy(logicalDevice)
        }
        pipelines.clear()

        for (b in renderCommandBuffers) {
            vkFreeCommandBuffers(logicalDevice.device, renderCommandPool.pool, b.buffer)
        }

        for (i in 0 until imageAcquiredSemaphore.size) {
            vkDestroySemaphore(logicalDevice.device, imageAcquiredSemaphore[i].semaphore, null)
            vkDestroySemaphore(logicalDevice.device, completeRenderSemaphore[i].semaphore, null)
            vkDestroyFence(logicalDevice.device, drawingFinishedFence[i][0], null)
        }
    }

    override fun render() {
        if(recreateSwapchain(vk.surface)) {
            recreateRenderCommandBuffers()
            return
        }

        var result = vkWaitForFences(logicalDevice.device, drawingFinishedFence[frameIndex], false, 1000000000)
        if (result != VK_SUCCESS) {
            log("Failed to wait for fence!")
        }

        result = vkResetFences(logicalDevice.device, drawingFinishedFence[frameIndex])
        if (result != VK_SUCCESS) {
            log("Failed to reset fence!")
        }

        val nextImage = swapchain.aquireNextImage(logicalDevice, imageAcquiredSemaphore[frameIndex])
        if (nextImage == -1) { // Need to recreate swapchain
            swapchainIsDirty = true
            return
        }

        renderCommandBuffers[frameIndex].begin()
        if (graphicsQueue[frameIndex].queue != presentQueue[frameIndex].queue) {
            attachPrePresentBarrier(renderCommandBuffers[frameIndex], swapchain.images[nextImage])
        }

        if (renderpass.isValid) {
            drawRenderPass(nextImage)
        }
        else {
            assertion("Renderpass is invalid when we want to use it!")
        }

        if (graphicsQueue[frameIndex].queue != presentQueue[frameIndex].queue) {
            attachPostPresentBarrier(renderCommandBuffers[frameIndex], swapchain.images[nextImage])
        }

        renderCommandBuffers[frameIndex].end()
        renderCommandBuffers[frameIndex].submit(graphicsQueue[frameIndex], imageAcquiredSemaphore[frameIndex], completeRenderSemaphore[frameIndex], VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, drawingFinishedFence[frameIndex])

        presentImage()

        frameIndex = (frameIndex+1)%swapchain.framebuffers.size
    }

    private fun drawRenderPass(nextImage: Int) {
        renderpass.begin(swapchain.framebuffers[nextImage], renderCommandBuffers[frameIndex], swapchain.extent)
        issueDrawingCommands()

        renderpass.end(renderCommandBuffers[frameIndex])
    }

    private fun issueDrawingCommands() {
        val obsoletePipelines = ArrayList<Pipeline>()
        // TODO: Performance: Don't update sceneData every frame (should contain mostly static stuff)
        val projectionMatrixBuffer = memAlloc(16 * 4)
        val pvMatrix = Matrix4f(camera.projection)
        pvMatrix.mul(camera.view)
        pvMatrix.get(projectionMatrixBuffer)

        for (draw in drawOpsQueue) {
            val mat = draw.material as VulkanMaterial
            val buffer = draw.vertexBuffer as VulkanVertexBuffer

            if (!mat.isValid || !buffer.isValid) {
                continue
            }

            var found = false
            for (pipeline in pipelines) {
                if (!pipeline.isValid) {
                    obsoletePipelines.add(pipeline)
                    continue
                }

                if (pipeline.matches(mat, buffer)) {
                    // If a materials texel buffer has changed we must recreate the descriptor sets
                    if (mat.hasTexelBuffer() && mat.texelBufferUniform.referencesHasChanged) {
                        mat.texelBufferUniform.referencesHasChanged = false
                        mat.descriptorPool.build(vk.logicalDevice)
                    }

                    pipeline.material.sceneData.update(projectionMatrixBuffer)
                    pipeline.begin(renderCommandBuffers[frameIndex])
                    pipeline.drawInstance(renderCommandBuffers[frameIndex], draw)
                    found = true
                }
            }

            if (!found) {
                val pipeline = Pipeline(mat, buffer.attributes, buffer.vertexPipelineVertexInputStateCreateInfo)
                pipeline.create(logicalDevice, renderpass)
                pipeline.material.sceneData.update(projectionMatrixBuffer)
                pipeline.begin(renderCommandBuffers[frameIndex])
                pipeline.drawInstance(renderCommandBuffers[frameIndex], draw)
                pipelines.add(pipeline)
            }
        }
        pipelines.removeAll(obsoletePipelines)
        drawOpsQueue.clear()
    }

    private fun presentImage() {
        pImageIndex.put(0, frameIndex)
        pSwapchains.put(0, swapchain.swapchain)
        pRenderCompleteSemaphore.put(0, completeRenderSemaphore[frameIndex].semaphore)

        val presentInfo = VkPresentInfoKHR.calloc()
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pNext(0)
                .pWaitSemaphores(pRenderCompleteSemaphore)
                .swapchainCount(pSwapchains.remaining())
                .pSwapchains(pSwapchains)
                .pImageIndices(pImageIndex)
                .pResults(null)

        val err = KHRSwapchain.vkQueuePresentKHR(presentQueue[frameIndex].queue, presentInfo)
        if (err != VK10.VK_SUCCESS) {
            assertion("Failed to present the swapchain image: " + VulkanResult(err))
        }
        else if (err == VK_ERROR_OUT_OF_DATE_KHR) {
            swapchainIsDirty = true
        }
    }

    private fun attachPrePresentBarrier(cmdBuffer: CommandPool.CommandBuffer, presentImage: Long) {
        val imageMemoryBarrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .pNext(MemoryUtil.NULL)
                .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                .oldLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .srcQueueFamilyIndex(queueFamilyIndices.presentFamily)
                .dstQueueFamilyIndex(queueFamilyIndices.graphicsFamily)

        imageMemoryBarrier.subresourceRange()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        imageMemoryBarrier.image(presentImage)

        VK10.vkCmdPipelineBarrier(cmdBuffer.buffer,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                0,
                null, // No memory barriers
                null, // No buffer memory barriers
                imageMemoryBarrier)

        imageMemoryBarrier.free()
    }

    private fun attachPostPresentBarrier(cmdBuffer: CommandPool.CommandBuffer, presentImage: Long) {
        val imageMemoryBarrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .pNext(MemoryUtil.NULL)
                .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
                .oldLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .srcQueueFamilyIndex(queueFamilyIndices.graphicsFamily)
                .dstQueueFamilyIndex(queueFamilyIndices.presentFamily)

        imageMemoryBarrier.subresourceRange()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        imageMemoryBarrier.image(presentImage)

        VK10.vkCmdPipelineBarrier(cmdBuffer.buffer,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                0,
                null, // No memory barriers
                null, // No buffer memory barriers
                imageMemoryBarrier)

        imageMemoryBarrier.free()
    }
}
