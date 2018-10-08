package rain.vulkan

import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import rain.api.Material
import rain.api.Texture2d

internal class VulkanMaterial(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, internal val vertexShader: ShaderModule, internal val fragmentShader: ShaderModule, internal val texture2d: VulkanTexture2d, internal val color: Vector3f) : Material {
    internal val descriptorPool: DescriptorPool
    internal val textureDataUBO = UniformBuffer()

    override fun getTexture2d(): Texture2d {
        return texture2d
    }

    init {
        val textureDataBuffer = memAlloc(2 * 4)
        val textureDataBufferF = textureDataBuffer.asFloatBuffer()
        textureDataBufferF.put(0, texture2d.texCoordWidth)
        textureDataBufferF.put(1, texture2d.texCoordHeight)
        textureDataUBO.create(logicalDevice, memoryProperties, BufferMode.SINGLE_BUFFER, textureDataBuffer.remaining().toLong())
        textureDataUBO.update(logicalDevice, textureDataBuffer, 0)
        descriptorPool = DescriptorPool()
                .withTexture(texture2d, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                .withUniformBuffer(textureDataUBO, VK10.VK_SHADER_STAGE_VERTEX_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                .build(logicalDevice)
    }
}
