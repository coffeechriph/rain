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

    var framebuffers = LongArray(0)
        private set

    var extent: VkExtent2D = VkExtent2D.create()
        private set

    lateinit var images: LongArray
        private set

    lateinit var imageViews: LongArray
        private set

    internal var depthImage = 0L
    internal var depthImageView = 0L

    private var depthImageMemory = 0L
    private val pImageIndex = memAllocInt(1)

    fun create(logicalDevice: LogicalDevice, physicalDevice: PhysicalDevice, surface: Surface, commandBuffer: CommandPool.CommandBuffer, deviceQueue: Queue, extent2D: VkExtent2D) {
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

        val preferredPresentMode = VK_PRESENT_MODE_MAILBOX_KHR
        var swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR
        for (i in 0 until presentModeCount) {
            if (pPresentModes.get(i) == preferredPresentMode) {
                swapchainPresentMode = preferredPresentMode
                break
            }
            if (swapchainPresentMode != preferredPresentMode && pPresentModes.get(i) == VK_PRESENT_MODE_FIFO_KHR) {
                log("Preferred present mode not available. Using VK_PRESENT_MODE_FIFO_KHR")
                swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR
            }
        }
        memFree(pPresentModes)

        // Determine the number of images
        var desiredNumberOfSwapchainImages = SWAPCHAIN_MODE.mode
        if (surfCaps.maxImageCount() in 1..(desiredNumberOfSwapchainImages - 1)) {
            desiredNumberOfSwapchainImages = surfCaps.maxImageCount()
        }

        val preTransform: Int
        preTransform = if (surfCaps.supportedTransforms() and VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR != 0) {
            VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR
        }
        else {
            surfCaps.currentTransform()
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

        images = LongArray(imageCount)
        imageViews = LongArray(imageCount)
        val pBufferView = memAllocLong(1)

        for (i in 0 until imageCount) {
            images[i] = pSwapchainImages.get(i)
            //transitionImageLayout(logicalDevice, pool, deviceQueue.queue, images[i], VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_UNDEFINED, VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

            val imageView = ImageView()
            imageView.create(logicalDevice, images[i], surface.format, VK_IMAGE_VIEW_TYPE_2D, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            imageViews[i] = imageView.imageView
            if (err != VK_SUCCESS) {
                assertion("Failed to create image view: " + VulkanResult(err))
            }
        }

        // Create depth image
        val depthFormat = findDepthFormat(physicalDevice)
        val imageCreateInfo: VkImageCreateInfo = VkImageCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .mipLevels(1)
                .arrayLayers(1)
                .format(depthFormat)
                .tiling(VK_IMAGE_TILING_OPTIMAL)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
                .flags(0)

        imageCreateInfo.extent()
            .width(extent2D.width())
            .height(extent2D.height())
            .depth(1) // Depth must be 1 if type is VK_IMAGE_TYPE_2D
        val pDepthImage = memAllocLong(1)
        vkCreateImage(logicalDevice.device, imageCreateInfo, null, pDepthImage)
        depthImage = pDepthImage.get()

        val memoryRequirements = VkMemoryRequirements.callocStack()
        vkGetImageMemoryRequirements(logicalDevice.device, depthImage, memoryRequirements)

        val typeIndex = memAllocInt(1)
        getMemoryType(physicalDevice.memoryProperties, memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, typeIndex)

        val memoryAllocateInfo = VkMemoryAllocateInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memoryRequirements.size())
                .memoryTypeIndex(typeIndex.get(0))

        val pDepthImageMemory = memAllocLong(1)
        err = vkAllocateMemory(logicalDevice.device, memoryAllocateInfo, null, pDepthImageMemory)
        if (err != VK_SUCCESS) {
            assertion("Error allocating depth memory " + VulkanResult(err))
        }

        depthImageMemory = pDepthImageMemory[0]
        vkBindImageMemory(logicalDevice.device, depthImage, depthImageMemory, 0)

        val dImageView = ImageView()
        dImageView.create(logicalDevice, depthImage, depthFormat, VK_IMAGE_VIEW_TYPE_2D, VK_IMAGE_ASPECT_DEPTH_BIT)
        depthImageView = dImageView.imageView
        transitionImageLayout(commandBuffer, deviceQueue.queue, depthImage, depthFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        memFree(pBufferView)
        memFree(pSwapchainImages)

        log("Created Swapchain[images: ${images.size}, presentMode: $swapchainPresentMode, format: ${surface.format}, colorSpace: ${surface.space}]")
    }

    fun createFramebuffers(logicalDevice: LogicalDevice, renderpass: Renderpass, extent: VkExtent2D) {
        val attachments = memAllocLong(2)
        val fci = VkFramebufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .pAttachments(attachments)
                .flags(0)
                .height(extent.height())
                .width(extent.width())
                .layers(1)
                .pNext(0)
                .renderPass(renderpass.handler)

        // Create a framebuffer for each swapchain image
        val framebuffers = LongArray(images.size)
        val pFramebuffer = memAllocLong(1)
        for (i in 0 until images.size) {
            attachments.put(0, imageViews[i])
            attachments.put(1, depthImageView)
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
        this.framebuffers = framebuffers
    }

    fun aquireNextImage(logicalDevice: LogicalDevice, semaphore: Semaphore): Int {
        val err = vkAcquireNextImageKHR(logicalDevice.device, swapchain, -1L, semaphore.semaphore, 0, pImageIndex)
        if (err == VK_ERROR_OUT_OF_DATE_KHR) {
            return -1
        }
        else if(err != VK_SUCCESS) {
            assertion("Failed to acquire next image from swapchain" + VulkanResult(err))
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
