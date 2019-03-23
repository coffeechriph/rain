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
import rain.api.components.GuiRenderComponent
import rain.api.components.RenderComponent
import rain.api.gfx.Renderer
import rain.api.manager.renderManagerInit
import rain.api.scene.Camera
import rain.assertion
import rain.log
import java.nio.ByteBuffer
import kotlin.collections.ArrayList

internal class VulkanRenderer (private val vk: Vk, val window: Window) : Renderer {
    private val pipelines: MutableList<Pipeline> = ArrayList()
    private val guiPipelines: MutableList<Pipeline> = ArrayList()

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

    private val activeRenderComponents = ArrayList<RenderComponent>()
    private val activeGuiRenderComponents = ArrayList<GuiRenderComponent>()

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
        renderManagerInit(this::addRenderComponent, this::removeRenderComponent,
                this::addGuiRenderComponent, this::removeGuiRenderComponent)
    }

    override fun setActiveCamera(camera: Camera) {
        this.camera = camera
    }

    override fun getDepthRange(): Vector2f {
        return Vector2f(0.0f, 20.0f)
    }

    private fun addRenderComponent(renderComponent: RenderComponent) {
        addRenderComponentInternal(renderComponent)
        activeRenderComponents.add(renderComponent)
    }

    private fun addGuiRenderComponent(guiRenderComponent: GuiRenderComponent) {
        addGuiRenderComponentInternal(guiRenderComponent)
        activeGuiRenderComponents.add(guiRenderComponent)
    }

    private fun addGuiRenderComponentInternal(guiRenderComponent: GuiRenderComponent) {
        val material = guiRenderComponent.material as VulkanMaterial
        val mesh = guiRenderComponent.mesh
        val vertexBuffer = mesh.vertexBuffer as VulkanVertexBuffer
        val pipeline = guiPipelines.stream()
                .filter{p -> p.matches(material, vertexBuffer)}
                .findFirst()
                .orElse(Pipeline(material, vertexBuffer.attributes, vertexBuffer.vertexPipelineVertexInputStateCreateInfo))

        if (material.hasTexelBuffer() && material.texelBufferUniform.referencesHasChanged) {
            material.texelBufferUniform.referencesHasChanged = false
            material.descriptorPool.build(vk.logicalDevice)
        }

        pipeline.guiRenderComponents.add(guiRenderComponent)
        if (!guiPipelines.contains(pipeline)) {
            pipeline.create(logicalDevice, renderpass)
            guiPipelines.add(pipeline)
        }
    }

    private fun addRenderComponentInternal(renderComponent: RenderComponent) {
        val mat = renderComponent.material as VulkanMaterial
        val buffer = renderComponent.mesh.vertexBuffer as VulkanVertexBuffer

        if (!mat.isValid || !buffer.isValid) {
            throw IllegalStateException("RenderComponents has invalid state [Material: ${mat.isValid}, Buffer: ${buffer.isValid}]")
        }

        var found = false
        for (pipeline in pipelines) {
            if (!pipeline.isValid) {
                continue
            }

            if (pipeline.matches(mat, buffer)) {
                // If a materials texel buffer has changed we must recreate the descriptor sets
                if (mat.hasTexelBuffer() && mat.texelBufferUniform.referencesHasChanged) {
                    mat.texelBufferUniform.referencesHasChanged = false
                    mat.descriptorPool.build(vk.logicalDevice)
                }

                pipeline.renderComponents.add(renderComponent)
                found = true
                break
            }
        }

        if (!found) {
            val pipeline = Pipeline(mat, buffer.attributes, buffer.vertexPipelineVertexInputStateCreateInfo)
            pipeline.create(logicalDevice, renderpass)
            pipeline.renderComponents.add(renderComponent)
            pipelines.add(pipeline)
        }
    }

    private fun removeRenderComponent(renderComponent: RenderComponent) {
        val mat = renderComponent.material as VulkanMaterial
        val buffer = renderComponent.mesh.vertexBuffer as VulkanVertexBuffer

        for (pipeline in pipelines) {
            if (pipeline.matches(mat, buffer)) {
                for (component2 in pipeline.renderComponents) {
                    if (renderComponent == component2) {
                        pipeline.renderComponents.remove(renderComponent)
                        activeRenderComponents.remove(renderComponent)
                        break
                    }
                }
                break
            }
        }
    }

    private fun removeGuiRenderComponent(guiRenderComponent: GuiRenderComponent) {
        val mat = guiRenderComponent.material as VulkanMaterial
        val buffer = guiRenderComponent.mesh.vertexBuffer as VulkanVertexBuffer
        for (pipeline in guiPipelines) {
            if (pipeline.matches(mat, buffer)) {
                for (component2 in pipeline.guiRenderComponents) {
                    if (guiRenderComponent == component2) {
                        pipeline.guiRenderComponents.remove(guiRenderComponent)
                        activeGuiRenderComponents.remove(guiRenderComponent)
                        break
                    }
                }
                break
            }
        }
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

            // Must re-add every active renderComponent as we've invalidated the pipelines
            for (component in activeRenderComponents) {
                addRenderComponentInternal(component)
            }

            for (component in activeGuiRenderComponents) {
                addGuiRenderComponentInternal(component)
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

        for (pipeline in guiPipelines) {
            pipeline.destroy(logicalDevice)
        }
        guiPipelines.clear()
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

        for (pipeline in guiPipelines) {
            pipeline.destroy(logicalDevice)
        }
        guiPipelines.clear()

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

        val pvMatrixBuffer = memAlloc(16 * 4)
        camera.view = Matrix4f()
        camera.view.translate(camera.x, camera.y, 0.0f)
        val pvMatrix = Matrix4f(camera.projection)
        pvMatrix.mul(camera.view)
        pvMatrix.get(pvMatrixBuffer)

        renderPipelines(pvMatrixBuffer)
        clearDepthBuffer(renderCommandBuffers[frameIndex])

        // We don't want the scene camera to influence the gui
        camera.projection.get(pvMatrixBuffer)
        renderGuiPipelines(pvMatrixBuffer)

        renderpass.end(renderCommandBuffers[frameIndex])
    }

    private fun renderPipelines(projectionMatrixBuffer: ByteBuffer) {
        val obsoletePipelines = ArrayList<Pipeline>()
        pipelines.sortBy { pipeline -> pipeline.material.blendEnabled }
        for (pipeline in pipelines) {
            if (!pipeline.isValid) {
                obsoletePipelines.add(pipeline)
                continue
            }

            val mat = pipeline.material

            // If a materials texel buffer has changed we must recreate the descriptor sets
            if (mat.hasTexelBuffer() && mat.texelBufferUniform.referencesHasChanged) {
                mat.texelBufferUniform.referencesHasChanged = false
                mat.descriptorPool.build(vk.logicalDevice)
            }

            mat.sceneData.update(projectionMatrixBuffer)
            pipeline.renderComponents.sortBy { component -> component.transform.z }
            pipeline.drawAll(renderCommandBuffers[frameIndex])
        }
        pipelines.removeAll(obsoletePipelines)
    }

    private fun renderGuiPipelines(projectionMatrixBuffer: ByteBuffer) {
        val obsoletePipelines = ArrayList<Pipeline>()
        for (pipeline in guiPipelines) {
            if (!pipeline.isValid) {
                obsoletePipelines.add(pipeline)
                continue
            }

            val mat = pipeline.material

            // If a materials texel buffer has changed we must recreate the descriptor sets
            if (mat.hasTexelBuffer() && mat.texelBufferUniform.referencesHasChanged) {
                mat.texelBufferUniform.referencesHasChanged = false
                mat.descriptorPool.build(vk.logicalDevice)
            }

            mat.sceneData.update(projectionMatrixBuffer)
            pipeline.drawAll(renderCommandBuffers[frameIndex])
        }
        guiPipelines.removeAll(obsoletePipelines)
    }

    private fun clearDepthBuffer(cmdBuffer: CommandPool.CommandBuffer) {
        val clearAttachment = VkClearAttachment.calloc(1)
        val depthStencil = VkClearDepthStencilValue.calloc()
                .depth(1.0f)
                .stencil(0)
        val clearValue = VkClearValue.calloc()
                .depthStencil(depthStencil)
        clearAttachment
                .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
                .clearValue(clearValue)
        val rect = VkRect2D.calloc()
        rect.extent()
                .width(window.framebufferSize.x)
                .height(window.framebufferSize.y)
        rect.offset()
                .set(0, 0)
        val clearRect = VkClearRect.calloc(1)
                .rect(rect)
                .layerCount(1)
        vkCmdClearAttachments(cmdBuffer.buffer, clearAttachment, clearRect)
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
