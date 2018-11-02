package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkImageViewCreateInfo
import rain.api.assertion

internal class ImageView {
    internal var imageView: Long = 0

    // TODO: Previously when creating the swapchain we did not recreate the VkImageViewCreateInfo
    // TODO: We could find a way to minimize allocating such structs every time
    internal fun create(logicalDevice: LogicalDevice, image: Long, imageFormat: Int, viewType: Int, aspectFlags: Int) {
        val colorAttachmentView = VkImageViewCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .pNext(MemoryUtil.NULL)
                .format(imageFormat)
                .viewType(viewType)
                .image(image)
                .flags(0)

        colorAttachmentView.components()
                .r(VK10.VK_COMPONENT_SWIZZLE_R)
                .g(VK10.VK_COMPONENT_SWIZZLE_G)
                .b(VK10.VK_COMPONENT_SWIZZLE_B)
                .a(VK10.VK_COMPONENT_SWIZZLE_A)

        colorAttachmentView.subresourceRange()
                .aspectMask(aspectFlags)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)

        val pBufferView = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateImageView(logicalDevice.device, colorAttachmentView, null, pBufferView)
        if (err != VK10.VK_SUCCESS) {
            assertion("Failed to create image view: " + VulkanResult(err))
        }

        imageView = pBufferView.get(0)

        colorAttachmentView.free()
    }
}
