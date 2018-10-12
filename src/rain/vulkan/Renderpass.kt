package rain.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkViewport
import rain.assertion


internal class Renderpass {
    var renderpass: Long = 0
        private set

    private var clearValues = VkClearValue.calloc(1)
    private var renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
    private var viewport = VkViewport.calloc(1)
    private var scissor = VkRect2D.calloc(1)

    fun create(logicalDevice: LogicalDevice, colorFormat: Int) {
        val colorAttachment = VkAttachmentDescription.calloc(1)
            .format(colorFormat)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

        val colorAttachmentRef = VkAttachmentReference.calloc(1)
            .attachment(0)
            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val subpass = VkSubpassDescription.calloc(1)
            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            .colorAttachmentCount(1)
            .pColorAttachments(colorAttachmentRef)

        val dependency = VkSubpassDependency.calloc(1)
            .srcSubpass(VK_SUBPASS_EXTERNAL)
            .dstSubpass(0)
            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

        val renderPassInfo = VkRenderPassCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            .pAttachments(colorAttachment)
            .pSubpasses(subpass)
            .pDependencies(dependency)

        renderpass = MemoryStack.stackPush().use {
            val pRenderPass = it.mallocLong(1)
            if(vkCreateRenderPass(logicalDevice.device, renderPassInfo, null, pRenderPass) != VK_SUCCESS)
                assertion("Could not create render pass")
            pRenderPass[0]
        }
    }

    fun setClearColor(red: Float, green: Float, blue: Float) {
        clearValues.color()
                .float32(0, red)
                .float32(1, green)
                .float32(2, blue)
                .float32(3, 1.0f)
    }

    fun begin(framebuffer: Long, cmdBuffer: CommandPool.CommandBuffer, viewportSize: VkExtent2D) {
        renderPassBeginInfo
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .pNext(0)
                .renderPass(renderpass)
                .pClearValues(clearValues)
                .framebuffer(framebuffer)

        renderPassBeginInfo.renderArea()
                .extent(viewportSize)
                .offset(VkOffset2D.create().set(0,0))

        vkCmdBeginRenderPass(cmdBuffer.buffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE)

        viewport.height(viewportSize.height().toFloat())
                .width(viewportSize.width().toFloat())
                .minDepth(0.0f)
                .maxDepth(1.0f)

        vkCmdSetViewport(cmdBuffer.buffer, 0, viewport)

        scissor.extent().set(viewportSize.width(), viewportSize.height())
        scissor.offset().set(0, 0)
        vkCmdSetScissor(cmdBuffer.buffer, 0, scissor)
    }

    fun end(cmdBuffer: CommandPool.CommandBuffer) {
        vkCmdEndRenderPass(cmdBuffer.buffer)
    }

    fun destroy(logicalDevice: LogicalDevice) {
        vkDestroyRenderPass(logicalDevice.device, renderpass, null)
    }
}
