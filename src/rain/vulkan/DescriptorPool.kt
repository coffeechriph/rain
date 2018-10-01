package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.LongBuffer

internal class DescriptorPool {
    var pool: Long = 0
    private var descriptorType: Int = 0

    fun create(logicalDevice: LogicalDevice, descriptorCount: Int, descriptorType: Int) {
        val descriptorPoolSize = VkDescriptorPoolSize.calloc(1)
                .type(descriptorType)
                .descriptorCount(descriptorCount)

        val descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(descriptorPoolSize)
                .maxSets(descriptorCount)

        val descriptorPool = memAllocLong(1)
        val err = vkCreateDescriptorPool(logicalDevice.device, descriptorPoolCreateInfo, null, descriptorPool)
        if (err != VK_SUCCESS) {
            throw AssertionError("Unable to create descriptor pool " + VulkanResult(err))
        }

        pool = descriptorPool.get(0)
        this.descriptorType = descriptorType
    }

    fun createUniformBufferSet(logicalDevice: LogicalDevice, stageFlags: Int, immutableSampler: LongBuffer?, descriptorCount: Int, uniformBuffer: UniformBuffer): DescriptorSet {
        val layout = createLayout(logicalDevice, stageFlags, immutableSampler)

        // We reuse the same layout, but are expecting an array of 1 layout per UBO
        val pLayouts = memAllocLong(descriptorCount);
        for (i in 0 until descriptorCount) {
            pLayouts.put(i, layout[0]);
        }

        val descriptorSetInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(pool)
                .pSetLayouts(pLayouts)

        val descriptorSets = memAllocLong(descriptorCount)
        val err = vkAllocateDescriptorSets(logicalDevice.device, descriptorSetInfo, descriptorSets)
        if (err != VK_SUCCESS) {
            throw AssertionError("Unable to create descriptor sets " + VulkanResult(err))
        }

        for (i in 0 until descriptorCount) {
            val bufferInfo = VkDescriptorBufferInfo.calloc(1)
                    .buffer(uniformBuffer.buffer[i])
                    .offset(0)
                    .range(VK_WHOLE_SIZE)

            val descriptorWrite = VkWriteDescriptorSet.calloc(1)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pBufferInfo(bufferInfo)

            vkUpdateDescriptorSets(logicalDevice.device, descriptorWrite, null)
        }

        return DescriptorSet(descriptorSets, layout[0], uniformBuffer)
    }

    private fun createLayout(logicalDevice: LogicalDevice, stageFlags: Int, immutableSampler: LongBuffer?): LongBuffer {
        // TODO: Change descriptorCount whenever we want to support more than 1 descriptor set / pipeline
        val descriptorLayout = VkDescriptorSetLayoutBinding.calloc(1)
                .descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .binding(0)
                .stageFlags(stageFlags)
                .pImmutableSamplers(immutableSampler)

        val descriptorSetCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(descriptorLayout)

        val descriptorSetLayout = memAllocLong(1)
        val err = vkCreateDescriptorSetLayout(logicalDevice.device, descriptorSetCreateInfo, null, descriptorSetLayout)
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to create descriptor set layout " + VulkanResult(err))
        }

        return descriptorSetLayout
    }

    class Builder {
        private val logicalDevice: LogicalDevice
        private val pool: DescriptorPool
        private val uniformBuffers = ArrayList<UniformBufferDescriptor>()
        private val textures = ArrayList<Texture2dDescriptor>()

        constructor(logicalDevice: LogicalDevice, pool: DescriptorPool) {
            this.logicalDevice = logicalDevice
            this.pool = pool
        }

        fun withUniformBuffer(uniformBuffer: UniformBuffer, stageFlags: Int): Builder {
            uniformBuffers.add(UniformBufferDescriptor(uniformBuffer, stageFlags))
            return this
        }

        fun withTexture2d(texture2d: VulkanTexture2d, stageFlags: Int): Builder {
            textures.add(Texture2dDescriptor(texture2d, stageFlags))
            return this
        }

        fun build(): List<DescriptorSet> {
            var bindingIndex = 0

            val descriptorSets = ArrayList<DescriptorSet>()
            for (u in uniformBuffers) {
                val ubo = createUniformBufferSet(logicalDevice, u.stageFlags, null, u.buffer.buffer.size, u.buffer, bindingIndex)
                bindingIndex += 1

                val descriptorSet = DescriptorSet(ubo.descriptorSet, ubo.layout, ubo.uniformBuffer)
                descriptorSets.add(descriptorSet)
            }

            for (t in textures) {

            }

            return descriptorSets
        }

        private fun createLayout(logicalDevice: LogicalDevice, stageFlags: Int, immutableSampler: LongBuffer?): LongBuffer {
            // TODO: Change descriptorCount whenever we want to support more than 1 descriptor set / pipeline
            val descriptorLayout = VkDescriptorSetLayoutBinding.calloc(1)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .binding(0)
                    .stageFlags(stageFlags)
                    .pImmutableSamplers(immutableSampler)

            val descriptorSetCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(descriptorLayout)

            val descriptorSetLayout = memAllocLong(1)
            val err = vkCreateDescriptorSetLayout(logicalDevice.device, descriptorSetCreateInfo, null, descriptorSetLayout)
            if (err != VK_SUCCESS) {
                throw AssertionError("Failed to create descriptor set layout " + VulkanResult(err))
            }

            return descriptorSetLayout
        }

        fun createUniformBufferSet(logicalDevice: LogicalDevice, stageFlags: Int, immutableSampler: LongBuffer?, descriptorCount: Int, uniformBuffer: UniformBuffer, bindingIndex: Int): DescriptorSet {
            val layout = createLayout(logicalDevice, stageFlags, immutableSampler)

            // We reuse the same layout, but are expecting an array of 1 layout per UBO
            val pLayouts = memAllocLong(descriptorCount);
            for (i in 0 until descriptorCount) {
                pLayouts.put(i, layout[0]);
            }

            val descriptorSetInfo = VkDescriptorSetAllocateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(pool.pool)
                    .pSetLayouts(pLayouts)

            val descriptorSets = memAllocLong(descriptorCount)
            val err = vkAllocateDescriptorSets(logicalDevice.device, descriptorSetInfo, descriptorSets)
            if (err != VK_SUCCESS) {
                throw AssertionError("Unable to create descriptor sets " + VulkanResult(err))
            }

            for (i in 0 until descriptorCount) {
                val bufferInfo = VkDescriptorBufferInfo.calloc(1)
                        .buffer(uniformBuffer.buffer[i])
                        .offset(0)
                        .range(VK_WHOLE_SIZE)

                val descriptorWrite = VkWriteDescriptorSet.calloc(1)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(descriptorSets[i])
                        .dstBinding(bindingIndex)
                        .dstArrayElement(0)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .pBufferInfo(bufferInfo)

                vkUpdateDescriptorSets(logicalDevice.device, descriptorWrite, null)
            }

            return DescriptorSet(descriptorSets, layout[0], uniformBuffer)
        }
    }

    private class UniformBufferDescriptor(val buffer: UniformBuffer, val stageFlags: Int)
    private class Texture2dDescriptor(val texture: VulkanTexture2d, val stageFlags: Int)
    class DescriptorSet(val descriptorSet: LongBuffer, val layout: Long, val uniformBuffer: UniformBuffer)
}
