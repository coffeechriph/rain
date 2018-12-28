package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.gfx.Drawable
import rain.assertion
import rain.log
import java.nio.LongBuffer
import java.util.*

internal class Pipeline(internal val material: VulkanMaterial, internal val vertexBuffer: VulkanVertexBuffer) {
    var isValid = false
        private set
        get() {
            if (!vertexBuffer.isValid) {
                return false
            }
            else if (!material.isValid) {
                return false
            }
            else if (indexBuffer != null && !indexBuffer!!.isValid) {
                return false
            }

            return field
        }

    private var pipeline: Long = 0
    private var pipelineLayout: Long = 0
    private var indexBuffer: VulkanIndexBuffer? = null
    private lateinit var pOffset: LongBuffer
    private lateinit var pBuffer: LongBuffer

    private val nextFrameDrawQueue = ArrayDeque<Drawable>()

    fun hasQueue() : Boolean {
        return nextFrameDrawQueue.peek() != null
    }
    fun matches(material: VulkanMaterial, vertexBuffer: VulkanVertexBuffer, indexBuffer: VulkanIndexBuffer?): Boolean {
        return this.material.id == material.id && this.vertexBuffer.id == vertexBuffer.id && (indexBuffer == null || this.indexBuffer!!.id == indexBuffer.id)
    }

    fun submitDrawInstance(draw: Drawable) {
        val vbuf = draw.vertexBuffer as VulkanVertexBuffer
        val mat = draw.material as VulkanMaterial
        val ibuf = if (draw.indexBuffer != null) {draw.indexBuffer as VulkanIndexBuffer} else {null}

        if (vbuf.id != vertexBuffer.id || mat.id != material.id || (ibuf != null && indexBuffer != null && ibuf.id != indexBuffer!!.id)) {
            assertion("A drawable that does not match the pipeline should never be submitted to it!")
        }

        nextFrameDrawQueue.add(draw)
    }

    fun create(logicalDevice: LogicalDevice, renderpass: Renderpass, indexBuffer: VulkanIndexBuffer?) {
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

        // TODO: Alpha blending should not be enabled by default
        val colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1)
                .blendEnable(true)
                .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
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

        val numDescriptorLayouts = material.descriptorPool.descriptorSets.size

        val descriptorSetLayouts = memAllocLong(numDescriptorLayouts)
        var i = 0
        for (set in material.descriptorPool.descriptorSets) {
            descriptorSetLayouts.put(i, set.layout)
            i += 1
        }

        // TODO: For now push constants allocate a constant size.
        val pushConstantRange = VkPushConstantRange.calloc(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .size(32 * 4)
                .offset(0)

        val pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pNext(0)
                .pSetLayouts(descriptorSetLayouts)
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
                .pVertexInputState(vertexBuffer.vertexPipelineVertexInputStateCreateInfo)
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
            assertion("Failed to create graphics pipeline ${VulkanResult(err)}")
        }

        pipeline = pPipelines.get(0)
        pipelineLayout = layout
        pBuffer = memAllocLong(1)
        pOffset = memAllocLong(1)
        this.indexBuffer = indexBuffer
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

    fun begin(cmdBuffer: CommandPool.CommandBuffer, nextFrame: Int) {
        vkCmdBindPipeline(cmdBuffer.buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
        pOffset.put(0, 0L)
        pBuffer.put(0, vertexBuffer.buffer)
        vkCmdBindVertexBuffers(cmdBuffer.buffer, 0, pBuffer, pOffset)

        if (indexBuffer != null) {
            vkCmdBindIndexBuffer(cmdBuffer.buffer, indexBuffer!!.buffer, 0, VK_INDEX_TYPE_UINT32)
        }

        val pDescriptorSet = memAllocLong(material.descriptorPool.descriptorSets.size)
        for (i in 0 until material.descriptorPool.descriptorSets.size) {
            if (material.descriptorPool.descriptorSets[i].bufferMode == BufferMode.SINGLE_BUFFER) {
                pDescriptorSet.put(i, material.descriptorPool.descriptorSets[i].descriptorSet[0])
            }
            else {
                pDescriptorSet.put(i, material.descriptorPool.descriptorSets[i].descriptorSet[nextFrame])
            }
        }
        vkCmdBindDescriptorSets(cmdBuffer.buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, pDescriptorSet, null)
    }

    fun drawAll(cmdBuffer: CommandPool.CommandBuffer) {
        if (indexBuffer == null) {
            while (nextFrameDrawQueue.peek() != null) {
                val drawable = nextFrameDrawQueue.pop()
                val pushData = drawable.uniformData

                vkCmdPushConstants(cmdBuffer.buffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushData)
                vkCmdDraw(cmdBuffer.buffer, vertexBuffer.vertexCount, 1, 0, 0)
            }
        }
        else {
            while (nextFrameDrawQueue.peek() != null) {
                val drawable = nextFrameDrawQueue.pop()
                val pushData = drawable.uniformData

                vkCmdPushConstants(cmdBuffer.buffer, pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushData)
                vkCmdDrawIndexed(cmdBuffer.buffer, indexBuffer!!.indexCount, 1, 0, 0, 0)
            }
        }
    }
}
