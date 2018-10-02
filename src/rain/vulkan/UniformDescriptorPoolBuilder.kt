package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import java.nio.LongBuffer

// TODO: Make it so that each DescriptorPools descriptors are bound to a separate binding
internal class UniformDescriptorPoolBuilder private constructor(val logicalDevice: LogicalDevice) {
    private var pool: Long = 0
    private val uniformBuffers = ArrayList<UniformBufferDescriptor>()

    internal class DescriptorSet(val descriptorSet: LongBuffer, val layout: Long, val uniformBuffer: UniformBuffer)
    internal class DescriptorPool constructor(val pool: Long, val descriptorSets: List<DescriptorSet>)
    internal class UniformBufferDescriptor(val buffer: UniformBuffer, val stageFlags: Int)

    companion object {
        fun create(logicalDevice: LogicalDevice): UniformDescriptorPoolBuilder {
            return UniformDescriptorPoolBuilder(logicalDevice)
        }
    }

    fun withUniformBuffer(uniformBuffer: UniformBuffer, stageFlags: Int): UniformDescriptorPoolBuilder {
        uniformBuffers.add(UniformBufferDescriptor(uniformBuffer, stageFlags))
        return this
    }

    fun build(): DescriptorPool {
        // We need to allocate enough space for every uniform buffer
        var descriptorCount = 0
        for (u in uniformBuffers) {
            descriptorCount += u.buffer.buffer.size
        }

        create(logicalDevice, descriptorCount)

        // TODO: UBOs over multiple bindings causes the SPV->MSL conversion to fail due to
        // duplicate buffer indices
        // Right now we span them across multiple sets instead..
        var bindingIndex = 0

        val descriptorSets = ArrayList<DescriptorSet>()
        for (u in uniformBuffers) {
            val descriptorSet = createUniformBufferSet(logicalDevice, u.stageFlags, bindingIndex, null, u.buffer)

            descriptorSets.add(descriptorSet)
        }

        return DescriptorPool(pool, descriptorSets)
    }

    private fun create(logicalDevice: LogicalDevice, descriptorCount: Int) {
        val descriptorPoolSize = VkDescriptorPoolSize.calloc(1)
                .type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(descriptorCount)

        val descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(descriptorPoolSize)
                .maxSets(descriptorCount)

        val descriptorPool = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateDescriptorPool(logicalDevice.device, descriptorPoolCreateInfo, null, descriptorPool)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Unable to create descriptor pool " + VulkanResult(err))
        }

        pool = descriptorPool.get(0)
    }

    private fun createLayout(logicalDevice: LogicalDevice, stageFlags: Int, bindingIndex: Int, immutableSampler: LongBuffer?): LongBuffer {
        // TODO: Change descriptorCount whenever we want to support more than 1 descriptor set / pipeline
        val descriptorLayout = VkDescriptorSetLayoutBinding.calloc(1)
                .descriptorCount(1)
                .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .binding(bindingIndex)
                .stageFlags(stageFlags)
                .pImmutableSamplers(immutableSampler)

        val descriptorSetCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(descriptorLayout)

        val descriptorSetLayout = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateDescriptorSetLayout(logicalDevice.device, descriptorSetCreateInfo, null, descriptorSetLayout)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create descriptor set layout " + VulkanResult(err))
        }

        return descriptorSetLayout
    }

    private fun createUniformBufferSet(logicalDevice: LogicalDevice, stageFlags: Int, bindingIndex: Int, immutableSampler: LongBuffer?, uniformBuffer: UniformBuffer): DescriptorSet {
        val layout = createLayout(logicalDevice, stageFlags, bindingIndex, immutableSampler)

        // We reuse the same layout, but are expecting an array of 1 layout per UBO
        val pLayouts = MemoryUtil.memAllocLong(uniformBuffer.buffer.size)
        for (i in 0 until uniformBuffer.buffer.size) {
            pLayouts.put(i, layout[0]);
        }

        val descriptorSetInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(pool)
                .pSetLayouts(pLayouts)

        val descriptorSets = MemoryUtil.memAllocLong(uniformBuffer.buffer.size)
        val err = VK10.vkAllocateDescriptorSets(logicalDevice.device, descriptorSetInfo, descriptorSets)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Unable to create descriptor sets " + VulkanResult(err))
        }

        for (i in 0 until uniformBuffer.buffer.size) {
            val bufferInfo = VkDescriptorBufferInfo.calloc(1)
                    .buffer(uniformBuffer.buffer[i])
                    .offset(0)
                    .range(VK10.VK_WHOLE_SIZE)

            val descriptorWrite = VkWriteDescriptorSet.calloc(1)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(bindingIndex)
                    .dstArrayElement(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pBufferInfo(bufferInfo)

            VK10.vkUpdateDescriptorSets(logicalDevice.device, descriptorWrite, null)
        }

        return DescriptorSet(descriptorSets, layout[0], uniformBuffer)
    }
}
