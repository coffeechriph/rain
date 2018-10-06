package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.Material
import rain.api.Renderer
import rain.api.SpriteComponent
import rain.api.TransformComponent

internal class VulkanRenderer (vk: Vk, val resourceFactory: VulkanResourceFactory) : Renderer {
    private val quadVertexBuffer: VertexBuffer
    private val pipelines: MutableList<Pipeline> = ArrayList()
    private val physicalDevice: PhysicalDevice = vk.physicalDevice
    private val logicalDevice: LogicalDevice = vk.logicalDevice
    private val renderpass: Renderpass = Renderpass()
    private val swapchain: Swapchain
    private val queueFamilyIndices: QueueFamilyIndices
    private var renderCommandPool: CommandPool = CommandPool()
    private var renderCommandBuffers: Array<CommandPool.CommandBuffer> = emptyArray()
    private var graphicsQueue = Array(2){Queue()}
    private var setupQueue = Queue()
    private var surfaceColorFormat = 0

    private lateinit var setupCommandPool: CommandPool
    private lateinit var setupCommandBuffer: CommandPool.CommandBuffer
    private lateinit var postPresentBuffer: CommandPool.CommandBuffer
    private lateinit var imageAcquiredSemaphore: Semaphore
    private lateinit var completeRenderSemaphore: Semaphore

    private var descPool = DescriptorPool()
    var swapchainIsDirty = true

    init {
        this.quadVertexBuffer = resourceFactory.quadVertexBuffer
        this.queueFamilyIndices = vk.queueFamilyIndices
        this.swapchain = Swapchain()
        this.surfaceColorFormat = vk.surface.format

        for(i in 0 until this.graphicsQueue.size) {
            this.graphicsQueue[i] = Queue()
            this.graphicsQueue[i].create(logicalDevice, queueFamilyIndices.graphicsFamily)
        }

        setupQueue = Queue()
        setupQueue.create(logicalDevice, vk.transferFamilyIndex)
    }

    // TODO: This one should be thread-safe but isn't really atm
    override fun submitDrawSprite(transform: TransformComponent, material: Material) {
        val mat = material as VulkanMaterial

        for (pipeline in pipelines) {
            if (pipeline.matchesShaderPair(mat.vertexShader.id,mat.fragmentShader.id)) {
                pipeline.addSpriteToDraw(transform)
                return
            }
        }

        val vertex = resourceFactory.getShader(mat.vertexShader.id) ?: throw IllegalStateException("Vertex shader with id ${mat.vertexShader.id} does not exist!")
        val fragment = resourceFactory.getShader(mat.fragmentShader.id) ?: throw IllegalStateException("Fragment shader with id ${mat.fragmentShader.id} does not exist!")

        val pipeline = Pipeline()
        pipeline.create(logicalDevice, renderpass, quadVertexBuffer, vertex, fragment, mat.descriptorPool)
        pipeline.addSpriteToDraw(transform)
        pipelines.add(pipeline)
    }

    override fun create() {
        renderpass.create(logicalDevice, surfaceColorFormat)
        renderCommandPool.create(logicalDevice, queueFamilyIndices.graphicsFamily)
        postPresentBuffer = renderCommandPool.createCommandBuffer(logicalDevice.device, 1)[0]

        setupCommandPool = CommandPool()
        setupCommandPool.create(logicalDevice, queueFamilyIndices.graphicsFamily)
        setupCommandBuffer = setupCommandPool.createCommandBuffer(logicalDevice.device, 1)[0]

        imageAcquiredSemaphore = Semaphore()
        imageAcquiredSemaphore.create(logicalDevice)
        completeRenderSemaphore = Semaphore()
        completeRenderSemaphore.create(logicalDevice)
    }

    fun destroy() {
        for (pipeline in pipelines) {
            pipeline.destroy()
        }
        renderpass.destroy()
        vkDestroySemaphore(logicalDevice.device, imageAcquiredSemaphore.semaphore, null)
        vkDestroySemaphore(logicalDevice.device, completeRenderSemaphore.semaphore, null)
    }

    internal fun recreateRenderCommandBuffers() {
        VK10.vkResetCommandPool(logicalDevice.device, renderCommandPool.pool, 0);
        renderCommandBuffers = renderCommandPool.createCommandBuffer(logicalDevice.device, swapchain.framebuffers!!.size)
    }

    fun recreateSwapchain(surface: Surface): Boolean {
        if (swapchainIsDirty) {
            val capabilities = VkSurfaceCapabilitiesKHR.calloc()
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.device, surface.surface, capabilities)

            swapchain.create(logicalDevice, physicalDevice, surface, setupCommandBuffer, setupQueue)
            swapchain.createFramebuffers(logicalDevice, renderpass, capabilities.currentExtent())
            swapchainIsDirty = false
            return true
        }

        return false
    }

    override fun render() {
        val nextImage = swapchain.aquireNextImage(logicalDevice, imageAcquiredSemaphore.semaphore)
        VK10.vkResetCommandBuffer(renderCommandBuffers[nextImage].buffer, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
        VK10.vkResetCommandBuffer(postPresentBuffer.buffer, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)

        renderCommandBuffers[nextImage].begin()
        renderpass.begin(swapchain.framebuffers!![nextImage], renderCommandBuffers[nextImage], swapchain.extent)

        for (pipeline in pipelines) {
            pipeline.begin(renderCommandBuffers[nextImage], pipeline.descriptorPool, nextImage)
            for (transform in pipeline.spriteList) {
                pipeline.draw(renderCommandBuffers[nextImage], transform)
            }
            pipeline.spriteList.clear()
        }

        attachPrePresentBarrier(renderCommandBuffers[nextImage], swapchain.images[nextImage])
        renderpass.end(renderCommandBuffers[nextImage])
        renderCommandBuffers[nextImage].end()
        renderCommandBuffers[nextImage].submit(graphicsQueue[nextImage].queue, imageAcquiredSemaphore, completeRenderSemaphore, VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)

        presentImage(nextImage)
    }

    private fun presentImage(nextImage: Int) {
        val pSwapchains = MemoryUtil.memAllocLong(1)
        val pRenderCompleteSemaphore = MemoryUtil.memAllocLong(1)
        val pImageIndex = MemoryUtil.memAllocInt(1)

        pImageIndex.put(0, nextImage)
        pSwapchains.put(0, swapchain.swapchain)
        pRenderCompleteSemaphore.put(0, completeRenderSemaphore.semaphore)

        val presentInfo = VkPresentInfoKHR.calloc()
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pNext(0)
                .pWaitSemaphores(pRenderCompleteSemaphore)
                .swapchainCount(pSwapchains.remaining())
                .pSwapchains(pSwapchains)
                .pImageIndices(pImageIndex)
                .pResults(null)

        val err = KHRSwapchain.vkQueuePresentKHR(graphicsQueue[nextImage].queue, presentInfo);
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to present the swapchain image: " + VulkanResult(err));
        }

        // TODO: Prefer waiting on fences instead
        // Would allow us to control the waiting more precisely
        VK10.vkQueueWaitIdle(graphicsQueue[nextImage].queue)

        submitPostPresentBarrier(postPresentBuffer, graphicsQueue[nextImage], swapchain.images[nextImage])
    }

    private fun attachPrePresentBarrier(cmdBuffer: CommandPool.CommandBuffer, presentImage: Long) {
        val imageMemoryBarrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .pNext(MemoryUtil.NULL)
                .srcAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dstAccessMask(0)
                .oldLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .newLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)

        imageMemoryBarrier.subresourceRange()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)
        imageMemoryBarrier.image(presentImage)

        VK10.vkCmdPipelineBarrier(cmdBuffer.buffer,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                0,
                null, // No memory barriers
                null, // No buffer memory barriers
                imageMemoryBarrier)

        imageMemoryBarrier.free();
    }

    private fun submitPostPresentBarrier(cmdBuffer: CommandPool.CommandBuffer, queue: Queue, presentImage: Long) {
        cmdBuffer.begin()

        val imageMemoryBarrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .pNext(MemoryUtil.NULL)
                .srcAccessMask(0)
                .dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .oldLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .newLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)

        imageMemoryBarrier.subresourceRange()
                .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)

        imageMemoryBarrier.image(presentImage)

        VK10.vkCmdPipelineBarrier(
                cmdBuffer.buffer,
                VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                0, // No memory barriers,
                null, null, // No buffer barriers,
                imageMemoryBarrier) // one image barrier
        imageMemoryBarrier.free()

        cmdBuffer.end()
        cmdBuffer.submit(queue.queue)
    }
}
