package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import rain.api.gfx.Material
import rain.api.gfx.Texture2d

internal class VulkanMaterial(val id: Long, val name: String, internal val vertexShader: ShaderModule, internal val fragmentShader: ShaderModule, internal val
texture2d: Array<Texture2d>, val logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, val depthWriteEnabled: Boolean = true) : Material {
    internal val descriptorPool: DescriptorPool
    internal val textureDataUBO = UniformBuffer()
    internal val sceneData = UniformBuffer()
    var isValid = false
        private set
        get() {
            for (texture in texture2d) {
                val t = texture as VulkanTexture2d
                if (!t.isValid) {
                    return false
                }
            }

            if (!vertexShader.isValid || !fragmentShader.isValid) {
                return false
            }

            return field
        }

    override fun getTexture2d(): Array<Texture2d> {
        return texture2d
    }

    override fun valid(): Boolean {
        return isValid
    }

    init {
        if (texture2d.isNotEmpty()) {
            val textureDataBuffer = memAlloc(2 * texture2d.size * 4)
            val textureDataBufferF = textureDataBuffer.asFloatBuffer()

            for (i in 0 until texture2d.size) {
                textureDataBufferF.put(0, texture2d[i].getTexCoordWidth())
                textureDataBufferF.put(1, texture2d[i].getTexCoordHeight())
            }
            textureDataUBO.create(logicalDevice, memoryProperties, BufferMode.SINGLE_BUFFER, textureDataBuffer.remaining().toLong())
            textureDataUBO.update(logicalDevice, textureDataBuffer, 0)
        }

        val sceneDataBuffer = memAlloc(16 * 4)
        val sceneDataBufferF = sceneDataBuffer.asFloatBuffer()
        sceneDataBufferF.put(floatArrayOf(  1.0f, 0.0f, 0.0f, 0.0f,
                                            0.0f, 1.0f, 0.0f, 0.0f,
                                            0.0f, 0.0f, 1.0f, 0.0f,
                                            0.0f, 0.0f, 0.0f, 1.0f))
        sceneDataBufferF.flip()
        sceneData.create(logicalDevice, memoryProperties, BufferMode.ONE_PER_SWAPCHAIN, sceneDataBuffer.remaining().toLong())
        sceneData.update(logicalDevice, sceneDataBuffer, 0)
        sceneData.update(logicalDevice, sceneDataBuffer, 1)
        descriptorPool = DescriptorPool()
        for (i in 0 until texture2d.size) {
            descriptorPool.withTexture(texture2d[i] as VulkanTexture2d, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        }

        descriptorPool
            .withUniformBuffer(sceneData, VK10.VK_SHADER_STAGE_VERTEX_BIT)

        if (texture2d.isNotEmpty()) {
            descriptorPool.withUniformBuffer(textureDataUBO, VK10.VK_SHADER_STAGE_VERTEX_BIT or VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
        }

        descriptorPool.build(logicalDevice)
        isValid = true
    }

    fun destroy() {
        isValid = false
        sceneData.destroy(logicalDevice)
        textureDataUBO.destroy(logicalDevice)
        descriptorPool.destroy(logicalDevice)
    }
}
