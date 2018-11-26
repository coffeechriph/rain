package rain.vulkan

import org.joml.Vector3f
import org.lwjgl.vulkan.VK10
import rain.api.gfx.*
import java.nio.ByteBuffer

internal class VulkanResourceFactory(val vk: Vk) : ResourceFactory {
    private var resourceId: Long = 0
    private val logicalDevice: LogicalDevice
    private val physicalDevice: PhysicalDevice
    private val queue: Queue
    private val commandPool: CommandPool
    internal val materials: MutableList<VulkanMaterial>
    private val textures: MutableMap<Long, VulkanTexture2d>
    private val shaders: MutableMap<Long, ShaderModule>
    private val buffers: MutableList<VulkanVertexBuffer>

    init {
        this.materials = ArrayList()
        this.textures = HashMap()
        this.shaders = HashMap()
        this.buffers = ArrayList()
        this.logicalDevice = vk.logicalDevice
        this.physicalDevice = vk.physicalDevice
        this.queue = vk.deviceQueue
        this.commandPool = CommandPool()
        this.commandPool.create(logicalDevice, vk.queueFamilyIndices.graphicsFamily)
    }

    fun getShader(id: Long): ShaderModule? {
        return shaders.get(id)
    }

    override fun createVertexBuffer(vertices: FloatArray, state: VertexBufferState, attributes: Array<VertexAttribute>): VulkanVertexBuffer {
        val buffer = VulkanVertexBuffer(uniqueId())
        buffer.create(vk, commandPool, vertices, attributes, state)
        buffers.add(buffer)
        return buffer
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

        val material = VulkanMaterial(uniqueId(), vertex, fragment, Array(1){texture2d}, color, logicalDevice, physicalDevice.memoryProperties)
        materials.add(material)

        return material
    }

    override fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, color: Vector3f): Material {
        val vertex = ShaderModule(uniqueId())
        val fragment = ShaderModule(uniqueId())

        // TODO: We should be able to actually load the shaders at a later time on the main thread
        // In order to make this method thread-safe
        vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)

        shaders.put(vertex.id, vertex)
        shaders.put(fragment.id, fragment)

        val material = VulkanMaterial(uniqueId(), vertex, fragment, texture2d, color, logicalDevice, physicalDevice.memoryProperties)
        materials.add(material)

        return material
    }

    // TODO: We should be able to actually load the texture at a later time on the main thread
    // In order to make this method thread-safe
    override fun loadTexture2d(textureFile: String, filter: TextureFilter): Texture2d {
        val texture2d = VulkanTexture2d()
        texture2d.load(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, textureFile, filter)
        textures.put(uniqueId(), texture2d)
        return texture2d
    }

    override fun createTexture2d(imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter): Texture2d {
        val texture2d = VulkanTexture2d()
        texture2d.createImage(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, imageData, width, height, channels, filter)
        textures.put(uniqueId(), texture2d)
        return texture2d
    }

    override fun clear() {
        for (material in materials) {
            material.destroy()
        }
        materials.clear()

        for (texture in textures) {
            texture.value.destroy(vk.logicalDevice)
        }
        textures.clear()

        for (shader in shaders) {
            shader.value.destroy(vk.logicalDevice)
        }
        shaders.clear()

        for (buffer in buffers) {
            buffer.destroy(vk.logicalDevice)
        }
        buffers.clear()
        resourceId = 0
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
