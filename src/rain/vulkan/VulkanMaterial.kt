package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL
import rain.api.gfx.BlendMode
import rain.api.gfx.Material
import rain.api.gfx.TexelBuffer
import rain.api.gfx.Texture2d
import rain.log

internal class VulkanMaterial(val vk: Vk,
                              private val setupCommandBuffer: CommandPool.CommandBuffer,
                              private val setupQueue: Queue,
                              val resourceFactory: VulkanResourceFactory,
                              val id: Long,
                              val name: String,
                              internal val vertexShader: ShaderModule,
                              internal val fragmentShader: ShaderModule,
                              internal val texture2d: Array<Texture2d>,
                              val depthWriteEnabled: Boolean = true,
                              val blendEnabled: Boolean = true,
                              val srcColor: BlendMode,
                              val dstColor: BlendMode,
                              val srcAlpha: BlendMode,
                              val dstAlpha: BlendMode) : Material {
    internal val descriptorPool: DescriptorPool
    internal val textureDataUBO = UniformBuffer(vk, setupCommandBuffer, setupQueue, resourceFactory)
    internal val sceneData = UniformBuffer(vk, setupCommandBuffer, setupQueue, resourceFactory)
    internal lateinit var texelBufferUniform: UniformTexelBuffer

    var batching = false
    var isValid = false
        private set
        get() {
            for (texture in texture2d) {
                val t = texture as VulkanTexture2d
                if (!t.isValid) {
                    log("Material $name has invalid texture!")
                    return false
                }
            }

            if (!vertexShader.isValid || !fragmentShader.isValid) {
                log("Material $name has invalid shaders!")
                return false
            }

            if (!descriptorPool.isValid) {
                log("Material $name has invalid descriptor pool!")
                return false
            }

            if (!textureDataUBO.isValid) {
                log("Material $name has invalid texture uniform buffer!")
                return false
            }

            if (!sceneData.isValid) {
                log("Material $name has invalid scene data!")
                return false
            }

            if (::texelBufferUniform.isInitialized && !texelBufferUniform.isValid) {
                log("Material $name has invalid texel buffer uniform!")
                return false
            }

            return field
        }

    internal fun hasTexelBuffer(): Boolean {
        return ::texelBufferUniform.isInitialized
    }

    override fun useBatching(): Boolean {
        return batching
    }

    override fun getTexelBuffer(): TexelBuffer {
        return texelBufferUniform
    }

    override fun copy(): Material {
        return resourceFactory.createMaterial(name + "copy", vertexShader, fragmentShader, texture2d[0], batching)
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
            textureDataUBO.create(textureDataBuffer.remaining().toLong())
            textureDataUBO.update(textureDataBuffer)
        }

        val sceneDataBuffer = memAlloc(16 * 4)
        val sceneDataBufferF = sceneDataBuffer.asFloatBuffer()
        sceneDataBufferF.put(floatArrayOf(  1.0f, 0.0f, 0.0f, 0.0f,
                                            0.0f, 1.0f, 0.0f, 0.0f,
                                            0.0f, 0.0f, 1.0f, 0.0f,
                                            0.0f, 0.0f, 0.0f, 1.0f))
        sceneDataBufferF.flip()
        sceneData.create(sceneDataBuffer.remaining().toLong())
        sceneData.update(sceneDataBuffer)
        descriptorPool = DescriptorPool()
        for (i in 0 until texture2d.size) {
            descriptorPool.withTexture(texture2d[i] as VulkanTexture2d, VK_SHADER_STAGE_ALL)
        }

        descriptorPool
            .withUniformBuffer(sceneData, VK_SHADER_STAGE_ALL)

        if (texture2d.isNotEmpty()) {
            descriptorPool.withUniformBuffer(textureDataUBO, VK_SHADER_STAGE_ALL)
        }

        log("Descriptor pool for material: $name")
        descriptorPool.build(vk.logicalDevice)
        isValid = true
    }

    fun invalidate() {
        isValid = false
    }
}
