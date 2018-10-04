package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.Entity
import rain.api.EntitySystem
import rain.api.Renderer

internal class VulkanRenderer (vk: Vk, val resourceFactory: VulkanResourceFactory) : Renderer {
    private val systems: MutableList<EntitySystem<Entity>> = ArrayList()
    private val quadVertexBuffer: VertexBuffer
    private val pipelines: MutableList<Pipeline> = ArrayList()
    private val physicalDevice: PhysicalDevice = vk.physicalDevice
    private val logicalDevice: LogicalDevice = vk.logicalDevice
    private val renderpass: Renderpass = Renderpass()
    private val swapchain: Swapchain
    private val queueFamilyIndices: QueueFamilyIndices
    private val uniformBufferTest = UniformBuffer()
    private val uniformBufferPosTest = UniformBuffer()
    private var uniformBufferPosTick = 0.0f
    private var renderCommandPool: CommandPool = CommandPool()
    private var renderCommandBuffers: Array<CommandPool.CommandBuffer> = emptyArray()
    private var queue: Queue
    private var surfaceColorFormat = 0
    private var textureTest = VulkanTexture2d()
    private lateinit var descriptorPoolTest2: DescriptorPool

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
        this.queue = vk.deviceQueue
        this.surfaceColorFormat = vk.surface.format
    }

    override fun newSystem(): EntitySystem<Entity> {
        val sys = EntitySystem<Entity>()
        systems.add(sys)
        return sys
    }

    override fun create() {
        renderpass.create(logicalDevice, surfaceColorFormat)
        renderCommandPool.create(logicalDevice, queueFamilyIndices.graphicsFamily)
        postPresentBuffer = renderCommandPool.createCommandBuffer(logicalDevice.device, 1)[0]

        uniformBufferTest.create(logicalDevice, physicalDevice.memoryProperties, 2, 16)

        val data = MemoryUtil.memAlloc(4 * 4)
        data.asFloatBuffer().put(0.5f).put(0.0f).put(0.5f).put(1.0f).flip()
        uniformBufferTest.update(logicalDevice, data, 0)
        uniformBufferTest.update(logicalDevice, data, 1)
        MemoryUtil.memFree(data)

        uniformBufferPosTest.create(logicalDevice, physicalDevice.memoryProperties, 1, 8)
        val data2 = MemoryUtil.memAlloc(2 * 4)
        data2.asFloatBuffer().put(0.25f).put(-0.25f).flip()
        uniformBufferPosTest.update(logicalDevice, data2, 0)
        MemoryUtil.memFree(data2)

        setupCommandPool = CommandPool()
        setupCommandPool.create(logicalDevice, queueFamilyIndices.graphicsFamily)
        setupCommandBuffer = setupCommandPool.createCommandBuffer(logicalDevice.device, 1)[0]
        textureTest.load(logicalDevice, physicalDevice.memoryProperties, setupCommandPool, queue.queue, "./data/textures/town.png")

        descPool = DescriptorPool()
            .withUniformBuffer(uniformBufferTest, VK_SHADER_STAGE_FRAGMENT_BIT)
            .withUniformBuffer(uniformBufferPosTest, VK_SHADER_STAGE_VERTEX_BIT)
            .withTexture(textureTest, VK_SHADER_STAGE_FRAGMENT_BIT)
            .build(logicalDevice)

        descriptorPoolTest2 = TextureDescriptorPoolBuilder.create(logicalDevice)
                .withTexture(textureTest, VK_SHADER_STAGE_FRAGMENT_BIT)
                .build(0)
    }

    internal fun recreateRenderCommandBuffers() {
        VK10.vkResetCommandPool(logicalDevice.device, renderCommandPool.pool, 0);
        renderCommandBuffers = renderCommandPool.createCommandBuffer(logicalDevice.device, swapchain.framebuffers!!.size)
    }

    fun recreateSwapchain(surface: Surface): Boolean {
        if (swapchainIsDirty) {
            val capabilities = VkSurfaceCapabilitiesKHR.calloc()
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.device, surface.surface, capabilities)

            swapchain.create(logicalDevice, physicalDevice, surface, setupCommandBuffer, queue)
            swapchain.createFramebuffers(logicalDevice, renderpass, capabilities.currentExtent())
            swapchainIsDirty = false
            return true
        }

        return false
    }

    override fun render() {
        uniformBufferPosTick += 0.005f

        // TODO: Remove me!
        val data2 = MemoryUtil.memAlloc(2 * 4)
        data2.asFloatBuffer().put(Math.sin(uniformBufferPosTick.toDouble()).toFloat() * 0.75f).put(Math.cos(uniformBufferPosTick.toDouble()).toFloat() * 0.75f).flip()
        uniformBufferPosTest.update(logicalDevice, data2, 0)
        MemoryUtil.memFree(data2)

        // TODO: Stupid way of doing this
        // But sufficient for now. Each material will spawn a separate pipeline
        if (pipelines.size < resourceFactory.materials.size) {
            val ln = resourceFactory.materials.size - pipelines.size
            for (i in 0 until ln) {
                val mat = resourceFactory.materials[resourceFactory.materials.size - ln + i]
                // TODO: Streamline the way we assign descriptor pools to pipelines
                val pipeline = Pipeline()
                pipeline.create(logicalDevice, renderpass, quadVertexBuffer, mat.vertexShader, mat.fragmentShader, descPool)
                pipelines.add(pipeline)
            }
        }

        for (system in systems) {
            for (update in system.updateIterator()) {
                update.update(system.findEntity(update.entity)!!)
            }
        }

        imageAcquiredSemaphore = Semaphore()
        imageAcquiredSemaphore.create(logicalDevice)
        completeRenderSemaphore = Semaphore()
        completeRenderSemaphore.create(logicalDevice)

        val nextImage = swapchain.aquireNextImage(logicalDevice, imageAcquiredSemaphore.semaphore)
        VK10.vkResetCommandBuffer(renderCommandBuffers[nextImage].buffer, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
        VK10.vkResetCommandBuffer(postPresentBuffer.buffer, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)

        renderCommandBuffers[nextImage].begin()
        renderpass.begin(swapchain.framebuffers!![nextImage], renderCommandBuffers[nextImage], swapchain.extent)

        for (pipeline in pipelines) {
            // TODO: Streamline the way we assign descriptorSets to pipelines
            val descriptorSets = LongArray(3)
            descriptorSets[0] = descPool.descriptorSets[0].descriptorSet[nextImage]
            descriptorSets[1] = descPool.descriptorSets[1].descriptorSet[0]
            descriptorSets[2] = descPool.descriptorSets[2].descriptorSet[0]

            pipeline.begin(renderCommandBuffers[nextImage], descriptorSets)
            pipeline.draw(renderCommandBuffers[nextImage])
        }

        attachPrePresentBarrier(renderCommandBuffers[nextImage], swapchain.images[nextImage])
        renderpass.end(renderCommandBuffers[nextImage])
        renderCommandBuffers[nextImage].end()
        renderCommandBuffers[nextImage].submit(queue.queue, imageAcquiredSemaphore, completeRenderSemaphore, VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)

        presentImage(nextImage)
        vkDestroySemaphore(logicalDevice.device, imageAcquiredSemaphore.semaphore, null)
        vkDestroySemaphore(logicalDevice.device, completeRenderSemaphore.semaphore, null)
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

        val err = KHRSwapchain.vkQueuePresentKHR(queue.queue, presentInfo);
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to present the swapchain image: " + VulkanResult(err));
        }

        // TODO: Prefer waiting on fences instead
        // Would allow us to control the waiting more precisely

        VK10.vkQueueWaitIdle(queue.queue)
        submitPostPresentBarrier(postPresentBuffer, queue, swapchain.images[nextImage])
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
        val cmdBufInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(MemoryUtil.NULL)
        var err = VK10.vkBeginCommandBuffer(cmdBuffer.buffer, cmdBufInfo)
        cmdBufInfo.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to begin command buffer: " + VulkanResult(err))
        }

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

        err = VK10.vkEndCommandBuffer(cmdBuffer.buffer)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to wait for idle queue: " + VulkanResult(err))
        }

        // Submit the command buffer
        cmdBuffer.submit(queue.queue)
    }
}
