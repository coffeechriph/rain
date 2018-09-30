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
    internal val quadVertexBuffer: VertexBuffer

    init {
        this.materials = ArrayList()
        this.textures = HashMap()
        this.logicalDevice = vk.logicalDevice
        this.physicalDevice = vk.physicalDevice
        this.queue = vk.deviceQueue
        this.commandPool = CommandPool()
        this.commandPool.create(logicalDevice, vk.queueFamilyIndices.graphicsFamily)

        val attributes = arrayOf(VertexAttribute(0, 2), VertexAttribute(1, 3))
        val vertices = floatArrayOf(
                -0.5f, -0.5f, 1.0f, 1.0f, 1.0f,
                -0.5f, 0.5f, 1.0f, 1.0f, 1.0f,
                0.5f, 0.5f, 1.0f, 1.0f, 1.0f,

                0.5f, 0.5f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.5f, 1.0f, 1.0f, 1.0f,
                -0.5f, -0.5f, 1.0f, 1.0f, 1.0f
        )

        quadVertexBuffer = VertexBuffer()
        quadVertexBuffer.create(logicalDevice, queue, commandPool, physicalDevice.memoryProperties, vertices, attributes, VertexBufferState.DYNAMIC)
    }

    override fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material {
        val vertex = ShaderModule()
        val fragment = ShaderModule()
        vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)

        val vid = uniqueId()
        val fid = uniqueId()

        val material = VulkanMaterial(vertex, fragment, texture2d, color)
        materials.add(material)

        return Material(vid, fid, texture2d, color)
    }

    override fun createTexture2d(textureFile: String, filter: TextureFilter): Texture2d {
        val texture2d = VulkanTexture2d()
        texture2d.load(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, textureFile)
        return Texture2d(0, 0, 0, filter)
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
