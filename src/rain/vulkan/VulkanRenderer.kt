package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import rain.api.Material
import rain.api.Renderer
import rain.api.SpriteComponent
import rain.api.TransformComponent
import java.nio.LongBuffer

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
    private var presentQueue = Array(2){Queue()}
    private var setupQueue = Queue()
    private var surfaceColorFormat = 0

    private lateinit var setupCommandPool: CommandPool
    private lateinit var setupCommandBuffer: CommandPool.CommandBuffer
    private lateinit var postPresentBuffer: CommandPool.CommandBuffer
    private lateinit var imageAcquiredSemaphore: Array<Semaphore>
    private lateinit var completeRenderSemaphore: Array<Semaphore>
    private lateinit var drawingFinishedFence: Array<LongBuffer>

    private val pSwapchains = MemoryUtil.memAllocLong(1)
    private val pRenderCompleteSemaphore = MemoryUtil.memAllocLong(1)
    private val pImageIndex = MemoryUtil.memAllocInt(1)

    var swapchainIsDirty = true
    var frameIndex = 0

    init {
        this.quadVertexBuffer = resourceFactory.quadVertexBuffer
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
    }

    fun destroy() {
        vkDeviceWaitIdle(logicalDevice.device)
        for (pipeline in pipelines) {
            pipeline.destroy()
        }
        renderpass.destroy()
        for (i in 0 until imageAcquiredSemaphore.size) {
            vkDestroySemaphore(logicalDevice.device, imageAcquiredSemaphore[i].semaphore, null)
            vkDestroySemaphore(logicalDevice.device, completeRenderSemaphore[i].semaphore, null)
        }
    }

    internal fun recreateRenderCommandBuffers() {
        VK10.vkResetCommandPool(logicalDevice.device, renderCommandPool.pool, 0);
        renderCommandBuffers = renderCommandPool.createCommandBuffer(logicalDevice.device, swapchain.framebuffers!!.size)

        completeRenderSemaphore = Array(swapchain.framebuffers!!.size){ Semaphore() }
        imageAcquiredSemaphore = Array(swapchain.framebuffers!!.size){ Semaphore() }
        drawingFinishedFence = Array(swapchain.framebuffers!!.size){ memAllocLong(1)}

        for (i in 0 until completeRenderSemaphore.size) {
            completeRenderSemaphore[i].create(logicalDevice)
            imageAcquiredSemaphore[i].create(logicalDevice)

            val fenceCreateInfo = VkFenceCreateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT)

            val err = vkCreateFence(logicalDevice.device, fenceCreateInfo, null, drawingFinishedFence[i])
            if (err != VK_SUCCESS) {
                throw AssertionError("Error creating fence " + VulkanResult(err))
            }
        }
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
        var result = vkWaitForFences(logicalDevice.device, drawingFinishedFence[frameIndex], false, 1000000000);
        if (result != VK_SUCCESS) {
            print("Failed to wait for fence!")
        }

        val nextImage = swapchain.aquireNextImage(logicalDevice, imageAcquiredSemaphore[frameIndex].semaphore)
        if (nextImage == -1) { // Need to recreate swapchain
            println("Swapchain dirty - recreate!")
            swapchainIsDirty = true
            return
        }

        //VK10.vkResetCommandBuffer(renderCommandBuffers[frameIndex].buffer, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
        //VK10.vkResetCommandBuffer(postPresentBuffer.buffer, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)

        renderCommandBuffers[frameIndex].begin()
        if (graphicsQueue[frameIndex].queue != presentQueue[frameIndex].queue) {
            attachPrePresentBarrier(renderCommandBuffers[frameIndex], swapchain.images[nextImage])
        }

        renderpass.begin(swapchain.framebuffers!![nextImage], renderCommandBuffers[frameIndex], swapchain.extent)

        for (pipeline in pipelines) {
            pipeline.begin(renderCommandBuffers[frameIndex], pipeline.descriptorPool, nextImage)
            for (transform in pipeline.spriteList) {
                pipeline.draw(renderCommandBuffers[frameIndex], transform)
            }
            pipeline.spriteList.clear()
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
        renderCommandBuffers[frameIndex].submit(graphicsQueue[frameIndex].queue, imageAcquiredSemaphore[frameIndex], completeRenderSemaphore[frameIndex], VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, drawingFinishedFence[frameIndex])

        presentImage(nextImage)

        frameIndex++
        if(frameIndex >= imageAcquiredSemaphore.size) {
            frameIndex = 0
        }
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

        val err = KHRSwapchain.vkQueuePresentKHR(presentQueue[frameIndex].queue, presentInfo);
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to present the swapchain image: " + VulkanResult(err));
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

        imageMemoryBarrier.free();
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

        imageMemoryBarrier.free();
    }
}
