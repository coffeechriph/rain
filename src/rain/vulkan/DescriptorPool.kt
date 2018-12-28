package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.assertion
import rain.log
import java.nio.LongBuffer

internal class DescriptorSet(val descriptorSet: LongBuffer, val layout: Long, val bufferMode: BufferMode)
internal class DescriptorPool {
    var descriptorSets = ArrayList<DescriptorSet>()
    var isValid = false
        private set
    var pool: Long = 0
        private set
    private var uniformBuffers = ArrayList<UniformBufferDescriptor>()
    private var textureDescriptors = ArrayList<TextureDescriptor>()

    internal class TextureDescriptor(val buffer: MutableList<VulkanTexture2d>, val stageFlags: Int)
    internal class UniformBufferDescriptor(val buffer: MutableList<UniformBuffer>, val stageFlags: Int)

    fun invalidate() {
        isValid = false
    }

    fun withUniformBuffer(uniformBuffer: UniformBuffer, stageFlags: Int): DescriptorPool {
        for (u in uniformBuffers) {
            if(u.stageFlags == stageFlags) {
                u.buffer.add(uniformBuffer)
                return this
            }
        }

        val uniform = UniformBufferDescriptor(ArrayList(), stageFlags)
        uniform.buffer.add(uniformBuffer)
        uniformBuffers.add(uniform)
        return this
    }

    fun withTexture(texture: VulkanTexture2d, stageFlags: Int): DescriptorPool {
        for (u in textureDescriptors) {
            if(u.stageFlags == stageFlags) {
                u.buffer.add(texture)
                return this
            }
        }

        val textures = TextureDescriptor(ArrayList(), stageFlags)
        textures.buffer.add(texture)
        textureDescriptors.add(textures)
        return this
    }

    fun build(logicalDevice: LogicalDevice): DescriptorPool {
        // We need to allocate enough space for every uniform buffer
        var descriptorCount = 0
        for (u in uniformBuffers) {
            for (k in u.buffer) {
                descriptorCount += k.buffer.size
            }
        }

        for (u in textureDescriptors) {
            descriptorCount += u.buffer.size
        }

        log("Descriptor count: $descriptorCount")
        create(logicalDevice, descriptorCount)

        val descriptorLayout = VkDescriptorSetLayoutBinding.calloc(descriptorCount)
        var layoutBufferIndex = 0
        var bindingIndex = 0
        log("CREATING DESCRIPTOR POOL")
        if (uniformBuffers.size > 0) {
            for (u in uniformBuffers) {
                for (k in u.buffer) {
                    for (l in k.buffer) {
                        log("Binding index: $bindingIndex")
                        descriptorLayout[layoutBufferIndex]
                                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                                .binding(bindingIndex)
                                .stageFlags(VK_SHADER_STAGE_ALL)
                                .pImmutableSamplers(null)
                        bindingIndex++
                        layoutBufferIndex++
                    }
                }
            }
        }

        if (textureDescriptors.size > 0) {
            for (u in textureDescriptors) {
                for (k in u.buffer) {
                    log("Binding index: $bindingIndex")
                    val sampler = memAllocLong(1)
                    sampler.put(0, k.textureSampler)
                    descriptorLayout[layoutBufferIndex]
                            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                            .binding(bindingIndex)
                            .stageFlags(VK_SHADER_STAGE_ALL)
                            .pImmutableSamplers(sampler)
                    bindingIndex++
                    layoutBufferIndex++
                }
            }
        }
        log("###### DESCRIPTOR POOL")

        for (b in descriptorLayout){
            log("BINDING ${b.binding()}")
        }

        val descriptorSetCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(descriptorLayout)

        val descriptorSetLayout = MemoryUtil.memAllocLong(1)
        log("PRE!!")
        val err = VK10.vkCreateDescriptorSetLayout(logicalDevice.device, descriptorSetCreateInfo, null, descriptorSetLayout)
        if (err != VK10.VK_SUCCESS) {
            assertion("Failed to create descriptor set layout " + VulkanResult(err))
        }
        log("POST!!!")
        val layout = descriptorSetLayout[0]

        bindingIndex = 0
        for (u in uniformBuffers) {
            for (k in u.buffer) {
                val descriptorSet = createUniformBufferSet(logicalDevice, layout, bindingIndex, k)
                descriptorSets.add(descriptorSet)
                bindingIndex += k.buffer.size
            }
        }

        for (u in textureDescriptors) {
            for (k in u.buffer) {
                val descriptorSet = createTextureDescriptorSet(logicalDevice, layout, bindingIndex, k)
                descriptorSets.add(descriptorSet)
                bindingIndex += 1
            }
        }

        isValid = true
        return this
    }

    private fun create(logicalDevice: LogicalDevice, descriptorCount: Int) {
        log("Pool size: ${uniformBuffers.size + textureDescriptors.size}")
        val descriptorPoolSize = VkDescriptorPoolSize.calloc(uniformBuffers.size + textureDescriptors.size)

        for (i in 0 until uniformBuffers.size) {
            // We need to be able to allocate 1 descriptor for each underlying buffer as well
            var dc = 0
            for (u in uniformBuffers[i].buffer) {
                dc += u.buffer.size
            }

            log("UBO Pool index $i - desc: $dc")
            descriptorPoolSize[i]
                .type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(dc)
        }

        for (i in 0 until textureDescriptors.size) {
            log("TBO Pool index ${i+uniformBuffers.size} - desc: ${textureDescriptors[i].buffer.size}")
            descriptorPoolSize[i + uniformBuffers.size]
                    .type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(3)
        }

        val descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(descriptorPoolSize)
                .maxSets(descriptorCount)

        log("Creating descriptor pool $descriptorCount")
        val descriptorPool = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateDescriptorPool(logicalDevice.device, descriptorPoolCreateInfo, null, descriptorPool)
        if (err != VK10.VK_SUCCESS) {
            assertion("Unable to create descriptor pool " + VulkanResult(err))
        }

        pool = descriptorPool.get(0)
    }

    private fun createTextureDescriptorSet(logicalDevice: LogicalDevice, layout: Long, bindingIndex: Int, texture: VulkanTexture2d): DescriptorSet {
        val pLayouts = MemoryUtil.memAllocLong(1)
        pLayouts.put(0, layout)

        val descriptorSetInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(pool)
                .pSetLayouts(pLayouts)

        val descriptorSets = MemoryUtil.memAllocLong(1)
        val err = VK10.vkAllocateDescriptorSets(logicalDevice.device, descriptorSetInfo, descriptorSets)
        if (err != VK10.VK_SUCCESS) {
            assertion("Unable to create descriptor sets " + VulkanResult(err))
        }

        val samplerInfo = VkDescriptorImageInfo.calloc(1)
                .sampler(texture.textureSampler)
                .imageView(texture.textureView)
                .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

        val descriptorWrite = VkWriteDescriptorSet.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(descriptorSets[0])
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .pImageInfo(samplerInfo)

        VK10.vkUpdateDescriptorSets(logicalDevice.device, descriptorWrite, null)
        return DescriptorSet(descriptorSets, layout, BufferMode.SINGLE_BUFFER)
    }

    private fun createUniformBufferSet(logicalDevice: LogicalDevice, layout: Long, bindingIndex: Int, uniformBuffer: UniformBuffer): DescriptorSet {
        // We reuse the same layout, but are expecting an array of 1 layout per UBO
        val pLayouts = MemoryUtil.memAllocLong(uniformBuffer.buffer.size)
        for (i in 0 until uniformBuffer.buffer.size) {
            pLayouts.put(i, layout);
        }

        val descriptorSetInfo = VkDescriptorSetAllocateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(pool)
                .pSetLayouts(pLayouts)

        val descriptorSets = MemoryUtil.memAllocLong(uniformBuffer.buffer.size)
        val err = VK10.vkAllocateDescriptorSets(logicalDevice.device, descriptorSetInfo, descriptorSets)
        if (err != VK10.VK_SUCCESS) {
            assertion("Unable to create descriptor sets " + VulkanResult(err))
        }

        for (i in 0 until uniformBuffer.buffer.size) {
            val bufferInfo = VkDescriptorBufferInfo.calloc(1)
                    .buffer(uniformBuffer.buffer[i])
                    .offset(0)
                    .range(VK10.VK_WHOLE_SIZE)

            val descriptorWrite = VkWriteDescriptorSet.calloc(1)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pBufferInfo(bufferInfo)

            VK10.vkUpdateDescriptorSets(logicalDevice.device, descriptorWrite, null)
        }

        return DescriptorSet(descriptorSets, layout, uniformBuffer.mode)
    }
}
