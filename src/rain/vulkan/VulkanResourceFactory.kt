package rain.vulkan

import org.joml.Vector3f
import org.lwjgl.vulkan.VK10
import rain.api.Material
import rain.api.ResourceFactory
import rain.api.Texture2d
import rain.api.TextureFilter

internal class VulkanResourceFactory(vk: Vk) : ResourceFactory {
    private var resourceId: Long = 0
    private val logicalDevice: LogicalDevice
    private val physicalDevice: PhysicalDevice
    private val queue: Queue
    private val commandPool: CommandPool
    internal val materials: MutableList<VulkanMaterial>
    private val textures: MutableMap<Long, VulkanTexture2d>
    private val shaders: MutableMap<Long, ShaderModule>
    internal val quadVertexBuffer: VertexBuffer

    init {
        this.materials = ArrayList()
        this.textures = HashMap()
        this.shaders = HashMap()
        this.logicalDevice = vk.logicalDevice
        this.physicalDevice = vk.physicalDevice
        this.queue = vk.deviceQueue
        this.commandPool = CommandPool()
        this.commandPool.create(logicalDevice, vk.queueFamilyIndices.graphicsFamily)

        val attributes = arrayOf(VertexAttribute(0, 2), VertexAttribute(1, 2))
        val vertices = floatArrayOf(
                -0.5f, -0.5f, 0.0f, 0.0f,
                -0.5f, 0.5f, 0.0f, 1.0f,
                0.5f, 0.5f, 1.0f, 1.0f,

                0.5f, 0.5f, 1.0f, 1.0f,
                0.5f, -0.5f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.0f, 0.0f
        )

        quadVertexBuffer = VertexBuffer()
        quadVertexBuffer.create(logicalDevice, queue, commandPool, physicalDevice.memoryProperties, vertices, attributes, VertexBufferState.DYNAMIC)
    }

    fun getShader(id: Long): ShaderModule? {
        return shaders.get(id)
    }

    // TODO: Let's think about if we want to take in a String for the texture instead and load it here...
    override fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material {
        val vertex = ShaderModule(uniqueId())
        val fragment = ShaderModule(uniqueId())

        // TODO: We should be able to actually load the shaders at a later time on the main thread
        // In order to make this method thread-safe
        vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)

        shaders.put(vertex.id, vertex)
        shaders.put(fragment.id, fragment)

        val material = VulkanMaterial(logicalDevice, vertex, fragment, texture2d as VulkanTexture2d, color)
        materials.add(material)

        return material
    }

    // TODO: We should be able to actually load the texture at a later time on the main thread
    // In order to make this method thread-safe
    override fun createTexture2d(textureFile: String, filter: TextureFilter): Texture2d {
        val texture2d = VulkanTexture2d()
        texture2d.load(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, textureFile)
        return texture2d
    }

    private fun uniqueId(): Long {
        if (resourceId + 1 == Long.MAX_VALUE) {
            throw IllegalStateException("There are no more Ids to generate for resources!")
        }

        val i = resourceId
        resourceId += 1

        return i
    }
}
