package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.LongBuffer

internal class Pipeline {
    var pipeline: Long = 0
        private set

    var pipelineLayout: Long = 0
        private set

    lateinit var vertexBuffer: VertexBuffer
        private set

    private lateinit var pOffset: LongBuffer // Used to temporarily store return values from vulkan
    private lateinit var pBuffer: LongBuffer

    fun create(logicalDevice: LogicalDevice, renderpass: Renderpass, vertexBuffer: VertexBuffer, vertexShader: ShaderModule, fragmentShader: ShaderModule, descriptorPool: DescriptorPool) {
        var err: Int
        // Vertex input state
        // Describes the topoloy used with this pipeline
        val inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)

        // Rasterization state
        val rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_NONE)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .depthBiasEnable(false)

        // Color blend state
        // Describes blend modes and color masks
        val colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1)
                .blendEnable(false)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT) // <- RGBA

        val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .pAttachments(colorWriteMask)

        // Viewport state
        val viewportState = VkPipelineViewportStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1) // <- one viewport
                .scissorCount(1) // <- one scissor rectangle

        // Enable dynamic states
        // Describes the dynamic states to be used with this pipeline
        // Dynamic states can be set even after the pipeline has been created
        // So there is no need to create new pipelines just for changing
        // a viewport's dimensions or a scissor box
        val pDynamicStates = memAllocInt(2)
        pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip()
        val dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
                // The dynamic state properties themselves are stored in the command buffer
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(pDynamicStates)

        // Depth and stencil state
        // Describes depth and stenctil test and compare ops
        val depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc()
                // No depth test/write and no stencil used
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(false)
                .depthWriteEnable(false)
                .depthCompareOp(VK_COMPARE_OP_ALWAYS)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false)

        depthStencilState.back()
                .failOp(VK_STENCIL_OP_KEEP)
                .passOp(VK_STENCIL_OP_KEEP)
                .compareOp(VK_COMPARE_OP_ALWAYS)
        depthStencilState.front(depthStencilState.back())

        // Multi sampling state
        // No multi sampling used in this example
        val multisampleState = VkPipelineMultisampleStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .pSampleMask(null)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

        val shaderStages = VkPipelineShaderStageCreateInfo.calloc(2)
        shaderStages.get(0).set(vertexShader.createInfo)
        shaderStages.get(1).set(fragmentShader.createInfo)

        // TODO: Change this when we want to support more than 1 descriptor set
        var numDescriptorLayouts = descriptorPool.descriptorSets.size

        val descriptorSetLayouts = memAllocLong(numDescriptorLayouts)
        var i = 0
        for (set in descriptorPool.descriptorSets) {
            descriptorSetLayouts.put(i, set.layout)
            i += 1
        }

        // Create the pipeline layout that is used to generate the rendering pipelines that
        // are based on this descriptor set layout
        val pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pNext(0)
                .pSetLayouts(descriptorSetLayouts)

        val pPipelineLayout = memAllocLong(1)
        err = vkCreatePipelineLayout(logicalDevice.device, pPipelineLayoutCreateInfo, null, pPipelineLayout)
        val layout = pPipelineLayout.get(0)
        memFree(pPipelineLayout)
        pPipelineLayoutCreateInfo.free()
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to create pipeline layout: " + VulkanResult(err))
        }

        // Assign states
        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .layout(layout)
                .renderPass(renderpass.renderpass)
                .pVertexInputState(vertexBuffer.vertexPipelineVertexInputStateCreateInfo)
                .pInputAssemblyState(inputAssemblyState)
                .pRasterizationState(rasterizationState)
                .pColorBlendState(colorBlendState)
                .pMultisampleState(multisampleState)
                .pViewportState(viewportState)
                .pDepthStencilState(depthStencilState)
                .pStages(shaderStages)
                .pDynamicState(dynamicState)

        // Create rendering pipeline
        val pPipelines = memAllocLong(1)
        err = vkCreateGraphicsPipelines(logicalDevice.device, VK_NULL_HANDLE, pipelineCreateInfo, null, pPipelines)
        pipeline = pPipelines.get(0)
        pipelineLayout = layout
        this.vertexBuffer = vertexBuffer
        pBuffer = memAllocLong(1)
        pOffset = memAllocLong(1)

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
            throw AssertionError("Failed to create pipeline: " + VulkanResult(err))
        }
    }

    fun destroy() {
        memFree(pBuffer)
        memFree(pOffset)
    }

    fun begin(cmdBuffer: CommandPool.CommandBuffer, descriptorPool: DescriptorPool, nextFrame: Int) {
        vkCmdBindPipeline(cmdBuffer.buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
        pOffset.put(0, 0L)
        pBuffer.put(0, vertexBuffer.buffer)
        vkCmdBindVertexBuffers(cmdBuffer.buffer, 0, pBuffer, pOffset)

        val pDescriptorSet = memAllocLong(descriptorPool.descriptorSets.size)
        for (i in 0 until descriptorPool.descriptorSets.size) {
            if (descriptorPool.descriptorSets[i].bufferMode == BufferMode.SINGLE_BUFFER) {
                pDescriptorSet.put(i, descriptorPool.descriptorSets[i].descriptorSet[0])
            }
            else {
                pDescriptorSet.put(i, descriptorPool.descriptorSets[i].descriptorSet[nextFrame])
            }
        }
        vkCmdBindDescriptorSets(cmdBuffer.buffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, pDescriptorSet, null)
    }

    fun draw(cmdBuffer: CommandPool.CommandBuffer) {
        vkCmdDraw(cmdBuffer.buffer, vertexBuffer.vertexCount, 1, 0, 0);
    }
}
