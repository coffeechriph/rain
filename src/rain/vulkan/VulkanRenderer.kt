package rain.vulkan

import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import rain.Window
import rain.api.*
import rain.assertion
import java.util.*

internal class VulkanRenderer (val vk: Vk, val window: Window) : Renderer {
    private data class DrawOp(val drawable: Drawable, val buffer: VulkanVertexBuffer)

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

    private val drawOpsQueue = ArrayDeque<DrawOp>()

    var swapchainIsDirty = true
    var frameIndex = 0

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

    override fun submitDraw(drawable: Drawable, vertexBuffer: VertexBuffer) {
        drawOpsQueue.add(DrawOp(drawable, vertexBuffer as VulkanVertexBuffer))
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

    internal fun recreateRenderCommandBuffers() {
        renderCommandBuffers = renderCommandPool.createCommandBuffer(logicalDevice.device, swapchain.framebuffers.size)
    }

    fun recreateSwapchain(surface: Surface): Boolean {
        if (swapchainIsDirty) {
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

            swapchain.create(logicalDevice, physicalDevice, surface, setupCommandPool, setupQueue, extent2D)
            renderpass.create(logicalDevice, surface.format, findDepthFormat(physicalDevice))
            pipelines.clear()
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

            return true
        }

        return false
    }

    private fun cleanUpResources() {
        for (i in 0 until swapchain.framebuffers.size) {
            vkDestroyFramebuffer(logicalDevice.device, swapchain.framebuffers[i], null)
        }
        vkDestroyImage(logicalDevice.device, swapchain.depthImage, null)
        vkDestroyImageView(logicalDevice.device, swapchain.depthImageView, null)

        for (b in renderCommandBuffers) {
            vkFreeCommandBuffers(logicalDevice.device, renderCommandPool.pool, b.buffer)
        }

        for (pipeline in pipelines) {
            pipeline.destroy(logicalDevice)
        }

        renderpass.destroy(logicalDevice)

        for (i in 0 until imageAcquiredSemaphore.size) {
            vkDestroySemaphore(logicalDevice.device, imageAcquiredSemaphore[i].semaphore, null)
            vkDestroySemaphore(logicalDevice.device, completeRenderSemaphore[i].semaphore, null)
            vkDestroyFence(logicalDevice.device, drawingFinishedFence[i][0], null)
        }
    }


    override fun render() {
        if(recreateSwapchain(vk.surface)) {
            recreateRenderCommandBuffers()
        }

        issueDrawingCommands()

        var result = vkWaitForFences(logicalDevice.device, drawingFinishedFence[frameIndex], false, 1000000000)
        if (result != VK_SUCCESS) {
            print("Failed to wait for fence!")
        }

        val nextImage = swapchain.aquireNextImage(logicalDevice, imageAcquiredSemaphore[frameIndex])
        if (nextImage == -1) { // Need to recreate swapchain
            swapchainIsDirty = true
            recreateSwapchain(vk.surface)
            recreateRenderCommandBuffers()
            return
        }

        renderCommandBuffers[frameIndex].begin()
        if (graphicsQueue[frameIndex].queue != presentQueue[frameIndex].queue) {
            attachPrePresentBarrier(renderCommandBuffers[frameIndex], swapchain.images[nextImage])
        }

        renderpass.begin(swapchain.framebuffers[nextImage], renderCommandBuffers[frameIndex], swapchain.extent)

        // TODO: Performance: Don't update sceneData every frame (should contain mostly static stuff)
        val projectionMatrixBuffer = memAlloc(16 * 4)
        camera.projection.get(projectionMatrixBuffer)
        for (pipeline in pipelines) {
            pipeline.material.sceneData.update(logicalDevice, projectionMatrixBuffer, nextImage)
            pipeline.begin(renderCommandBuffers[frameIndex], pipeline.descriptorPool, nextImage)
            pipeline.drawAll(renderCommandBuffers[frameIndex])
        }

        renderpass.end(renderCommandBuffers[frameIndex])

        if (graphicsQueue[frameIndex].queue != presentQueue[frameIndex].queue) {
            attachPostPresentBarrier(renderCommandBuffers[frameIndex], swapchain.images[nextImage])
        }

        renderCommandBuffers[frameIndex].end()

        result = vkResetFences(logicalDevice.device, drawingFinishedFence[frameIndex])
        if (result != VK_SUCCESS) {
            print("Failed to reset fence!")
        }
        renderCommandBuffers[frameIndex].submit(graphicsQueue[frameIndex], imageAcquiredSemaphore[frameIndex], completeRenderSemaphore[frameIndex], VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, drawingFinishedFence[frameIndex])

        presentImage(nextImage)

        frameIndex++
        if(frameIndex >= imageAcquiredSemaphore.size) {
            frameIndex = 0
        }
    }

    private fun issueDrawingCommands() {
        // Sort the order of rendering so alpha tests are correct
        val sortedListOfDraw = drawOpsQueue.sortedBy { it.drawable.getTransform().position.z }
        for (draw in sortedListOfDraw) {
            val mat = draw.drawable.getMaterial() as VulkanMaterial
            var found = false
            for (pipeline in pipelines) {
                if (pipeline.vertexBuffer == draw.buffer && pipeline.material == mat) {
                    pipeline.submitDrawInstance(draw.drawable)
                    found = true
                }
            }

            if (!found) {
                val material = draw.drawable.getMaterial() as VulkanMaterial
                val pipeline = Pipeline()
                pipeline.create(logicalDevice, renderpass, draw.buffer, material, material.descriptorPool)
                pipeline.submitDrawInstance(draw.drawable)
                pipelines.add(pipeline)
            }
        }
        drawOpsQueue.clear()
    }

    private fun presentImage(nextImage: Int) {
        pImageIndex.put(0, nextImage)
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
