package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
import java.nio.LongBuffer

// TODO: Current limitation is that there's 1 DescriptorPool per type of Descriptor set (Texture or Uniform buffer)
internal class TextureDescriptorPoolBuilder private constructor(val logicalDevice: LogicalDevice) {
    private var pool: Long = 0
    private val textureDescriptors = ArrayList<TextureDescriptor>()

    internal class TextureDescriptor(val buffer: VulkanTexture2d, val stageFlags: Int)

    companion object {
        fun create(logicalDevice: LogicalDevice): TextureDescriptorPoolBuilder {
            return TextureDescriptorPoolBuilder(logicalDevice)
        }
    }

    fun withTexture(texture: VulkanTexture2d, stageFlags: Int): TextureDescriptorPoolBuilder {
        textureDescriptors.add(TextureDescriptor(texture, stageFlags))
        return this
    }

    fun build(bindingIndex: Int): DescriptorPool {
        // We need to allocate enough space for every texture sampler
        var descriptorCount = textureDescriptors.size

        create(logicalDevice, descriptorCount)

        val descriptorSets = ArrayList<DescriptorSet>()
        for (tex in textureDescriptors) {
            val descriptorSet = createTextureDescriptorSet(logicalDevice, tex.stageFlags, bindingIndex, null, tex.buffer)

            descriptorSets.add(descriptorSet)
        }

        return DescriptorPool(pool, descriptorSets)
    }

    private fun create(logicalDevice: LogicalDevice, descriptorCount: Int) {
        val descriptorPoolSize = VkDescriptorPoolSize.calloc(1)
                .type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
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
                .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
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

    private fun createTextureDescriptorSet(logicalDevice: LogicalDevice, stageFlags: Int, bindingIndex: Int, immutableSampler: LongBuffer?, texture: VulkanTexture2d): DescriptorSet {
        val layout = createLayout(logicalDevice, stageFlags, bindingIndex, immutableSampler)

        // We reuse the same layout, but are expecting an array of 1 layout per UBO
        val pLayouts = MemoryUtil.memAllocLong(1)
        pLayouts.put(0, layout[0]);

        val descriptorSetInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(pool)
                .pSetLayouts(pLayouts)

        val descriptorSets = MemoryUtil.memAllocLong(1)
        val err = VK10.vkAllocateDescriptorSets(logicalDevice.device, descriptorSetInfo, descriptorSets)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Unable to create descriptor sets " + VulkanResult(err))
        }

        val samplerInfo = VkDescriptorImageInfo.calloc(1)
                .sampler(texture.textureSampler)
                .imageView(texture.textureView)
                .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

        val descriptorWrite = VkWriteDescriptorSet.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(descriptorSets[0])
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .pImageInfo(samplerInfo)

        VK10.vkUpdateDescriptorSets(logicalDevice.device, descriptorWrite, null)

        return DescriptorSet(descriptorSets, layout[0])
    }
}
