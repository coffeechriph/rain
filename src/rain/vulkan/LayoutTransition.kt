package rain.vulkan

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageMemoryBarrier
import org.lwjgl.vulkan.VkQueue

internal fun transitionImageLayout(commandBuffer: CommandPool.CommandBuffer, queue: VkQueue, image: Long, format: Int, oldLayout: Int, newLayout: Int) {
    commandBuffer.begin()

    val imageBarrier = VkImageMemoryBarrier.calloc(1)
            .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .oldLayout(oldLayout)
            .newLayout(newLayout)
            .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
            .image(image)

    var sourceStage = 0
    var dstStage = 0

    if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
        imageBarrier
                .srcAccessMask(0)
                .dstAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)

        sourceStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
        dstStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT
    }
    else if(oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
        imageBarrier
                .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT)

        sourceStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT
        dstStage = VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
    }
    else if(oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
        imageBarrier
                .srcAccessMask(0)
                .dstAccessMask(VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT or VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
        sourceStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
        dstStage = VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT
    }

    var aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
    if (newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
        aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT
        if (hasStencilComponent(format)) {
            aspectMask = aspectMask or VK_IMAGE_ASPECT_STENCIL_BIT
        }
    }

    imageBarrier.subresourceRange()
            .aspectMask(aspectMask)
            .baseMipLevel(0)
            .levelCount(1)
            .baseArrayLayer(0)
            .layerCount(1)

    VK10.vkCmdPipelineBarrier(commandBuffer.buffer, sourceStage, dstStage, 0, null, null, imageBarrier)
    commandBuffer.end()
    commandBuffer.submit(queue)
    VK10.vkQueueWaitIdle(queue)
}
