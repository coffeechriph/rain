package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.assertion
import rain.log
import java.nio.LongBuffer

internal class DescriptorSet(val descriptorSet: LongBuffer, val layout: Long, val tag: String)
internal class DescriptorPool {
    var isValid = false
        private set
    var pool: Long = 0
        private set
    var descriptorSet: Long = 0
        private set
    var descriptorSetLayout: Long = 0
        private set
    private var uniformBuffers = ArrayList<UniformBufferDescriptor>()
    private var textureDescriptors = ArrayList<TextureDescriptor>()

    internal class TextureDescriptor(val buffer: VulkanTexture2d, val stageFlags: Int)
    internal class UniformBufferDescriptor(val buffer: UniformBuffer, val stageFlags: Int)

    fun invalidate() {
        isValid = false
    }

    fun withUniformBuffer(uniformBuffer: UniformBuffer, stageFlags: Int): DescriptorPool {
        val uniform = UniformBufferDescriptor(uniformBuffer, stageFlags)
        uniformBuffers.add(uniform)
        return this
    }

    fun withTexture(texture: VulkanTexture2d, stageFlags: Int): DescriptorPool {
        val textures = TextureDescriptor(texture, stageFlags)
        textureDescriptors.add(textures)
        return this
    }

    fun build(logicalDevice: LogicalDevice): DescriptorPool {
        val descriptorSetLayoutBinding = VkDescriptorSetLayoutBinding.calloc(uniformBuffers.size + textureDescriptors.size)
        var bindingIndex = 0
        for (uniform in uniformBuffers) {
            descriptorSetLayoutBinding[bindingIndex]
                    .binding(bindingIndex)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_ALL)
                    .pImmutableSamplers(null)
            bindingIndex += 1
        }

        for (texture in textureDescriptors) {
            val immutableSampler = memAllocLong(1)
            immutableSampler.put(0, texture.buffer.textureSampler)
            descriptorSetLayoutBinding[bindingIndex]
                    .binding(bindingIndex)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_ALL)
                    .pImmutableSamplers(immutableSampler)
        }

        // Descriptor set create info
        val descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pNext(0)
                .pBindings(descriptorSetLayoutBinding)

        val pDescriptorSetLayout = memAllocLong(1);
        var error = vkCreateDescriptorSetLayout(logicalDevice.device, descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout)
        if (error != VK_SUCCESS) {
            assertion("Vulkan error: ${VulkanResult(error)}")
        }

        descriptorSetLayout = pDescriptorSetLayout[0]

        // CREATE POOL - One for UNIFORM_BUFFER and SAMPLER types
        val descriptorPoolSize = VkDescriptorPoolSize.calloc(2)
        descriptorPoolSize[0]
                .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(uniformBuffers.size)
        descriptorPoolSize[1]
                .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(textureDescriptors.size)

        val descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pNext(0)
                .maxSets(1)
                .pPoolSizes(descriptorPoolSize)

        val pDescriptorPool = memAllocLong(1)
        error = vkCreateDescriptorPool(logicalDevice.device, descriptorPoolCreateInfo, null, pDescriptorPool)
        if (error != VK_SUCCESS) {
            assertion("Vulkan error: ${VulkanResult(error)}")
        }
        pool = pDescriptorPool[0]
        memFree(pDescriptorPool)

        val descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .pNext(0)
                .descriptorPool(pool)
                .pSetLayouts(pDescriptorSetLayout)

        val pDescriptorSet = memAllocLong(1)
        error = vkAllocateDescriptorSets(logicalDevice.device, descriptorSetAllocateInfo, pDescriptorSet)
        if (error != VK_SUCCESS) {
            assertion("Vulkan error: ${VulkanResult(error)}")
        }

        descriptorSet = pDescriptorSet[0]

        // Write
        val writeDescriptorSet = VkWriteDescriptorSet.calloc(uniformBuffers.size+textureDescriptors.size)

        bindingIndex = 0
        for (i in 0 until uniformBuffers.size) {
            val pBufferInfo = VkDescriptorBufferInfo.calloc(1)
                    .buffer(uniformBuffers[i].buffer.buffer)
                    .offset(0)
                    .range(VK10.VK_WHOLE_SIZE)

            writeDescriptorSet[i]
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .pNext(0)
                    .dstSet(pDescriptorSet[0])
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pBufferInfo(pBufferInfo)
                    .dstArrayElement(0)
                    .dstBinding(bindingIndex)
            bindingIndex += 1
        }

        for (i in 0 until textureDescriptors.size) {
                val samplerInfo = VkDescriptorImageInfo.calloc(1)
                        .sampler(textureDescriptors[i].buffer.textureSampler)
                        .imageView(textureDescriptors[i].buffer.textureView)
                        .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

                writeDescriptorSet[i+uniformBuffers.size]
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .pNext(0)
                    .dstSet(pDescriptorSet[0])
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImageInfo(samplerInfo)
                    .dstArrayElement(0)
                    .dstBinding(bindingIndex)
            bindingIndex += 1
        }

        vkUpdateDescriptorSets(logicalDevice.device, writeDescriptorSet, null)

        isValid = true
        return this
    }
}
