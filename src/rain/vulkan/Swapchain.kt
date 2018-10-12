package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import rain.assertion
import rain.log

internal class Swapchain {
    enum class SwapchainMode(internal val mode: Int) {
        SINGLE_BUFFERED(1),
        DOUBLE_BUFFERED(2);
    }

    companion object {
        internal var SWAPCHAIN_MODE = SwapchainMode.DOUBLE_BUFFERED
    }

    var swapchain: Long = 0
        private set

    lateinit var images: Array<Long>
        private set

    var imageViews = Array<Long>(0){0}
        private set

    var framebuffers: Array<Long>? = null
        private set

    var extent: VkExtent2D = VkExtent2D.create()
        private set

    private val pImageIndex = memAllocInt(1)

    fun create(logicalDevice: LogicalDevice, physicalDevice: PhysicalDevice, surface: Surface, cmdbuffer: CommandPool.CommandBuffer, deviceQueue: Queue, extent2D: VkExtent2D) {
        var err: Int
        extent = VkExtent2D.create()
                .width(extent2D.width())
                .height(extent2D.height())

        // Get physical device surface properties and formats
        val surfCaps = VkSurfaceCapabilitiesKHR.calloc()
        err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.device, surface.surface, surfCaps)
        if (err != VK_SUCCESS) {
            assertion("Failed to get physical device surface capabilities: " + VulkanResult(err))
        }

        val pPresentModeCount = memAllocInt(1)
        err = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.device, surface.surface, pPresentModeCount, null)
        val presentModeCount = pPresentModeCount.get(0)
        if (err != VK_SUCCESS) {
            assertion("Failed to get number of physical device surface presentation modes: " + VulkanResult(err))
        }

        val pPresentModes = memAllocInt(presentModeCount)
        err = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.device, surface.surface, pPresentModeCount, pPresentModes)
        memFree(pPresentModeCount)
        if (err != VK_SUCCESS) {
            assertion("Failed to get physical device surface presentation modes: " + VulkanResult(err))
        }

        val preferredPresentMode = VK_PRESENT_MODE_FIFO_KHR
        var swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR
        for (i in 0 until presentModeCount) {
            if (pPresentModes.get(i) == preferredPresentMode) {
                swapchainPresentMode = preferredPresentMode
                break
            }
            if (swapchainPresentMode != preferredPresentMode && pPresentModes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR) {
                println("Preferred present mode not available. Using VK_PRESENT_MODE_IMMEDIATE_KHR")
                swapchainPresentMode = VK_PRESENT_MODE_IMMEDIATE_KHR
            }
        }
        memFree(pPresentModes)

        // Determine the number of images
        var desiredNumberOfSwapchainImages = SWAPCHAIN_MODE.mode
        if (surfCaps.maxImageCount() > 0 && desiredNumberOfSwapchainImages > surfCaps.maxImageCount()) {
            desiredNumberOfSwapchainImages = surfCaps.maxImageCount()
        }

        val preTransform: Int
        if (surfCaps.supportedTransforms() and VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR != 0) {
            preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR
        }
        else {
            preTransform = surfCaps.currentTransform()
        }
        surfCaps.free()

        val swapchainCI = VkSwapchainCreateInfoKHR.calloc()
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .pNext(NULL)
                .surface(surface.surface)
                .minImageCount(desiredNumberOfSwapchainImages)
                .imageFormat(surface.format)
                .imageColorSpace(surface.space)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(preTransform)
                .imageArrayLayers(1)
                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null)
                .presentMode(swapchainPresentMode)
                .oldSwapchain(swapchain)
                .clipped(true)
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)

        swapchainCI.imageExtent()
                .width(extent2D.width())
                .height(extent2D.height())

        val pSwapChain = memAllocLong(1)
        err = vkCreateSwapchainKHR(logicalDevice.device, swapchainCI, null, pSwapChain)
        swapchainCI.free()
        val newSwapChain = pSwapChain.get(0)
        memFree(pSwapChain)
        if (err != VK_SUCCESS) {
            assertion("Failed to create swap chain: " + VulkanResult(err))
        }

        // If we just re-created an existing swapchain, we should destroy the old swapchain at this point.
        // Note: destroying the swapchain also cleans up all its associated presentable images once the platform is done with them.
        if (newSwapChain > 0) {
            vkDestroySwapchainKHR(logicalDevice.device, swapchain, null)
            swapchain = newSwapChain
        }

        val pImageCount = memAllocInt(1)
        err = vkGetSwapchainImagesKHR(logicalDevice.device, swapchain, pImageCount, null)
        val imageCount = pImageCount.get(0)
        if (err != VK_SUCCESS) {
            assertion("Failed to get number of swapchain images: " + VulkanResult(err))
        }

        val pSwapchainImages = memAllocLong(imageCount)
        err = vkGetSwapchainImagesKHR(logicalDevice.device, swapchain, pImageCount, pSwapchainImages)
        if (err != VK_SUCCESS) {
            assertion("Failed to get swapchain images: " + VulkanResult(err))
        }
        memFree(pImageCount)

        images = LongArray(imageCount).toTypedArray()
        imageViews = LongArray(imageCount).toTypedArray()
        val pBufferView = memAllocLong(1)

        cmdbuffer.begin()
        for (i in 0 until imageCount) {
            images[i] = pSwapchainImages.get(i)
            // Bring the image from an UNDEFINED state to the VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT state
            imageBarrier(cmdbuffer.buffer, images[i], VK_IMAGE_ASPECT_COLOR_BIT,
                    VK_IMAGE_LAYOUT_UNDEFINED, 0,
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

            val imageView = ImageView()
            imageView.create(logicalDevice, images[i], surface.format, VK_IMAGE_VIEW_TYPE_2D)
            imageViews[i] = imageView.imageView
            if (err != VK_SUCCESS) {
                assertion("Failed to create image view: " + VulkanResult(err))
            }
        }
        cmdbuffer.end()
        cmdbuffer.submit(deviceQueue.queue)
        vkQueueWaitIdle(deviceQueue.queue)

        memFree(pBufferView)
        memFree(pSwapchainImages)

        log("Created Swapchain[images: ${images.size}, presentMode: $swapchainPresentMode, format: ${surface.format}, colorSpace: ${surface.space}]")
    }

    private fun imageBarrier(cmdbuffer: VkCommandBuffer, image: Long, aspectMask: Int, oldImageLayout: Int, srcAccess: Int, newImageLayout: Int, dstAccess: Int) {
        // Create an image barrier object
        val imageMemoryBarrier = VkImageMemoryBarrier.calloc(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .pNext(0)
                .oldLayout(oldImageLayout)
                .srcAccessMask(srcAccess)
                .newLayout(newImageLayout)
                .dstAccessMask(dstAccess)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image)

        imageMemoryBarrier.subresourceRange()
                .aspectMask(aspectMask)
                .baseMipLevel(0)
                .levelCount(1)
                .layerCount(1)

        // Put barrier on top
        val srcStageFlags = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
        val destStageFlags = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT

        // Put barrier inside setup command buffer
        vkCmdPipelineBarrier(cmdbuffer, srcStageFlags, destStageFlags, 0, // no memory barriers
                null, null, // no buffer memory barriers
                imageMemoryBarrier) // one image memory barrier
        imageMemoryBarrier.free()
    }

    fun createFramebuffers(logicalDevice: LogicalDevice, renderpass: Renderpass, extent: VkExtent2D) {
        val attachments = memAllocLong(1)
        val fci = VkFramebufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .pAttachments(attachments)
                .flags(0)
                .height(extent.height())
                .width(extent.width())
                .layers(1)
                .pNext(0)
                .renderPass(renderpass.renderpass)

        // Create a framebuffer for each swapchain image
        val framebuffers = LongArray(images.size)
        val pFramebuffer = memAllocLong(1)
        for (i in 0 until images.size) {
            attachments.put(0, imageViews[i])
            val err = vkCreateFramebuffer(logicalDevice.device, fci, null, pFramebuffer)
            val framebuffer = pFramebuffer.get(0)
            if (err != VK_SUCCESS) {
                assertion("Failed to create framebuffer: " + VulkanResult(err))
            }
            framebuffers[i] = framebuffer
        }
        memFree(attachments)
        memFree(pFramebuffer)
        fci.free()
        this.framebuffers = framebuffers.toTypedArray()
    }

    // TODO: Accept Semaphore instead of long
    fun aquireNextImage(logicalDevice: LogicalDevice, semaphore: Long): Int {
        val err = vkAcquireNextImageKHR(logicalDevice.device, swapchain, -1L, semaphore, 0, pImageIndex)
        if (err != VK_SUCCESS) {
            assertion("Failed to acquire next image from swapchain " + VulkanResult(err))
        }
        else if (err == VK_ERROR_OUT_OF_DATE_KHR) {
            return -1
        }
        return pImageIndex.get(0)
    }

    fun destroy(logicalDevice: LogicalDevice) {
        for (i in imageViews) {
            vkDestroyImageView(logicalDevice.device, i, null)
        }
        vkDestroySwapchainKHR(logicalDevice.device, swapchain, null)
    }
}
