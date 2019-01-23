package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.entity.RenderComponent
import rain.api.gfx.Drawable
import rain.assertion
import rain.log
import java.nio.LongBuffer

internal class Pipeline(internal val material: VulkanMaterial, private val vertexFormat: Array<VertexAttribute>, private val vertexInputCreateInfo: VkPipelineVertexInputStateCreateInfo) {
    var isValid = false
        private set
        get() {
            if (!material.isValid) {
                return false
            }

            return field
        }

    private var pipeline: Long = 0
    private var pipelineLayout: Long = 0
    private lateinit var pOffset: LongBuffer
    private lateinit var pBuffer: LongBuffer

    val renderComponents = ArrayList<RenderComponent>()

    fun matches(material: VulkanMaterial, vertexBuffer: VulkanVertexBuffer): Boolean {
        return this.material.id == material.id && vertexFormat.contentEquals(vertexBuffer.attributes)
    }

    fun create(logicalDevice: LogicalDevice, renderpass: Renderpass) {
        var err: Int
        val inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)

        val rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_NONE)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .depthBiasEnable(false)
                .lineWidth(1.0f)

        // TODO: Alpha blending should not be enabled by default
        val colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1)
                .blendEnable(material.blendEnabled)
                .srcColorBlendFactor(material.srcColor.value)
                .dstColorBlendFactor(material.dstColor.value)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(material.srcAlpha.value)
                .dstAlphaBlendFactor(material.dstAlpha.value)
                .alphaBlendOp(VK_BLEND_OP_ADD)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT) // <- RGBA

        val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .pAttachments(colorWriteMask)

        val viewportState = VkPipelineViewportStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .scissorCount(1)

        val pDynamicStates = memAllocInt(2)
        pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip()
        val dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(pDynamicStates)

        val depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(true)
                .depthWriteEnable(material.depthWriteEnabled)
                .depthCompareOp(VK_COMPARE_OP_LESS)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false)

        val multisampleState = VkPipelineMultisampleStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .pSampleMask(null)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

        val shaderStages = VkPipelineShaderStageCreateInfo.calloc(2)
        shaderStages.get(0).set(material.vertexShader.createInfo)
        shaderStages.get(1).set(material.fragmentShader.createInfo)

        val pDescriptorSetLayout = memAllocLong(1)
        pDescriptorSetLayout.put(0, material.descriptorPool.descriptorSetLayout)

        // TODO: For now push constants allocate a constant size.
        val pushConstantRange = VkPushConstantRange.calloc(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .size(32 * 4)
                .offset(0)

        val pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pNext(0)
                .pSetLayouts(pDescriptorSetLayout)
                .pPushConstantRanges(pushConstantRange)

        val pPipelineLayout = memAllocLong(1)
        err = vkCreatePipelineLayout(logicalDevice.device, pPipelineLayoutCreateInfo, null, pPipelineLayout)
        val layout = pPipelineLayout.get(0)
        memFree(pPipelineLayout)
        pPipelineLayoutCreateInfo.free()
        if (err != VK_SUCCESS) {
            assertion("Failed to create pipeline layout: " + VulkanResult(err))
        }

        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .layout(layout)
                .renderPass(renderpass.handler)
                .pVertexInputState(vertexInputCreateInfo)
                .pInputAssemblyState(inputAssemblyState)
                .pRasterizationState(rasterizationState)
                .pColorBlendState(colorBlendState)
                .pMultisampleState(multisampleState)
                .pViewportState(viewportState)
                .pDepthStencilState(depthStencilState)
                .pStages(shaderStages)
                .pDynamicState(dynamicState)

        val pPipelines = memAllocLong(1)
        err = vkCreateGraphicsPipelines(logicalDevice.device, VK_NULL_HANDLE, pipelineCreateInfo, null, pPipelines)
        if (err != VK_SUCCESS) {
            log("Material: ${material.name}")
            assertion("Failed to create graphics pipeline ${VulkanResult(err)}")
        }

        pipeline = pPipelines.get(0)
        pipelineLayout = layout
        pBuffer = memAllocLong(1)
        pOffset = memAllocLong(1)
        this.isValid = true

        shaderStages.free()
        multisampleState.free()
        depthStencilState.free()
        dynamicState.free()
        memFree(pDynamicStates)
        viewportState.free()
        colorBlendState.free()
        colorWriteMask.free()
        rasterizationState.free()
        inputAssemblyState.free()
        if (err != VK_SUCCESS) {
            assertion("Failed to create pipeline: " + VulkanResult(err))
        }
    }

    fun destroy(logicalDevice: LogicalDevice) {
        vkDestroyPipeline(logicalDevice.device, pipeline, null)
        vkDestroyPipelineLayout(logicalDevice.device, pipelineLayout, null)
        memFree(pBuffer)
        memFree(pOffset)
        isValid = false
    }

    fun drawAll(cmdBuffer: CommandPool.CommandBuffer) {
        begin(cmdBuffer)
        for (component in renderComponents) {
            if (!component.visible) {
                continue
            }

            if (component.mesh.indexBuffer == null) {
                val vbo = component.mesh.vertexBuffer as VulkanVertexBuffer
                val pushData = component.createUniformData()

                pOffset.put(0, 0L)
                pBuffer.put(0, vbo.rawBuffer.buffer)

                vkCmdBindVertexBuffers(cmdBuffer.buffer, 0, pBuffer, pOffset)

                vkCmdPushConstants(cmdBuffer.buffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushData)
                vkCmdDraw(cmdBuffer.buffer, vbo.vertexCount, 1, 0, 0)
            }
            else {
                val pushData = component.createUniformData()
                val vbo = component.mesh.vertexBuffer as VulkanVertexBuffer
                val ibo = component.mesh.indexBuffer as VulkanIndexBuffer

                pOffset.put(0, 0L)
                pBuffer.put(0, vbo.rawBuffer.buffer)

                vkCmdBindVertexBuffers(cmdBuffer.buffer, 0, pBuffer, pOffset)
                vkCmdBindIndexBuffer(cmdBuffer.buffer, ibo.rawBuffer.buffer, 0, VK_INDEX_TYPE_UINT32)

                vkCmdPushConstants(cmdBuffer.buffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushData)
                vkCmdDrawIndexed(cmdBuffer.buffer, ibo.indexCount, 1, 0, 0, 0)
            }
        }
    }

    fun begin(cmdBuffer: CommandPool.CommandBuffer) {
        vkCmdBindPipeline(cmdBuffer.buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)

        val pDescriptorSet = memAllocLong(1)
        pDescriptorSet.put(0, material.descriptorPool.descriptorSet)
        vkCmdBindDescriptorSets(cmdBuffer.buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, pDescriptorSet, null)
    }

    fun drawInstance(cmdBuffer: CommandPool.CommandBuffer, drawable: Drawable) {
        if (drawable.indexBuffer == null) {
            val vbo = drawable.vertexBuffer as VulkanVertexBuffer
            val pushData = drawable.uniformData

            pOffset.put(0, 0L)
            pBuffer.put(0, vbo.rawBuffer.buffer)

            vkCmdBindVertexBuffers(cmdBuffer.buffer, 0, pBuffer, pOffset)

            vkCmdPushConstants(cmdBuffer.buffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushData)
            vkCmdDraw(cmdBuffer.buffer, vbo.vertexCount, 1, 0, 0)
        }
        else {
            val pushData = drawable.uniformData
            val vbo = drawable.vertexBuffer as VulkanVertexBuffer
            val ibo = drawable.indexBuffer as VulkanIndexBuffer

            pOffset.put(0, 0L)
            pBuffer.put(0, vbo.rawBuffer.buffer)

            vkCmdBindVertexBuffers(cmdBuffer.buffer, 0, pBuffer, pOffset)
            vkCmdBindIndexBuffer(cmdBuffer.buffer, ibo.rawBuffer.buffer, 0, VK_INDEX_TYPE_UINT32)

            vkCmdPushConstants(cmdBuffer.buffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushData)
            vkCmdDrawIndexed(cmdBuffer.buffer, ibo.indexCount, 1, 0, 0, 0)
        }
    }
}
