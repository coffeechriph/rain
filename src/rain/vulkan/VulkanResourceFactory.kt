package rain.vulkan

import org.joml.Vector3f
import org.lwjgl.vulkan.VK10
import rain.assertion
import rain.api.gfx.*
import rain.log
import java.nio.ByteBuffer

internal class VulkanResourceFactory(val vk: Vk, val renderer: VulkanRenderer) : ResourceFactory {
    private var resourceId: Long = 0
    private val logicalDevice: LogicalDevice
    private val physicalDevice: PhysicalDevice
    private val queue: Queue
    private val commandPool: CommandPool
    internal val materials: MutableList<VulkanMaterial>
    private val textures: MutableMap<String, VulkanTexture2d>
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

    override fun createVertexBuffer(vertices: FloatArray, state: VertexBufferState, attributes: Array<VertexAttribute>): VulkanVertexBuffer {
        log("Creating vertex buffer of size ${vertices.size * 4} bytes.")
        val buffer = VulkanVertexBuffer(uniqueId())
        buffer.create(vk, commandPool, vertices, attributes, state)
        buffers.add(buffer)
        return buffer
    }

    // TODO: Let's think about if we want to take in a String for the texture instead and load it here...
    override fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material {
        log("Creating material from sources (vertex: $vertexShaderFile, fragment: $fragmentShaderFile) with texture $texture2d")
        val vertex = ShaderModule(uniqueId())
        val fragment = ShaderModule(uniqueId())

        // TODO: We should be able to actually load the shaders at a later time on the main thread
        // In order to make this method thread-safe
        vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)

        shaders.put(vertex.id, vertex)
        shaders.put(fragment.id, fragment)

        val material = VulkanMaterial(uniqueId(), name, vertex, fragment, Array(1){texture2d}, color, logicalDevice, physicalDevice.memoryProperties)
        materials.add(material)

        return material
    }

    override fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, color: Vector3f): Material {
        log("Creating material from sources (vertex: $vertexShaderFile, fragment: $fragmentShaderFile) with texture $texture2d")
        val vertex = ShaderModule(uniqueId())
        val fragment = ShaderModule(uniqueId())

        // TODO: We should be able to actually load the shaders at a later time on the main thread
        // In order to make this method thread-safe
        vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
        fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)

        shaders.put(vertex.id, vertex)
        shaders.put(fragment.id, fragment)

        val material = VulkanMaterial(uniqueId(), name, vertex, fragment, texture2d, color, logicalDevice, physicalDevice.memoryProperties)
        materials.add(material)

        return material
    }

    // TODO: We should be able to actually load the texture at a later time on the main thread
    // In order to make this method thread-safe
    override fun loadTexture2d(name: String, textureFile: String, filter: TextureFilter): Texture2d {
        log("Loading texture $name from $textureFile with filter $filter.")
        val texture2d = VulkanTexture2d(uniqueId())
        texture2d.load(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, textureFile, filter)
        textures[name] = texture2d
        return texture2d
    }

    override fun createTexture2d(name: String, imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter): Texture2d {
        log("Creating texture $name from source with filter $filter.")
        val texture2d = VulkanTexture2d(uniqueId())
        texture2d.createImage(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, imageData, width, height, channels, filter)
        textures[name] = texture2d
        return texture2d
    }

    // Deleting a material involves removing any pipeline which may reference it.
    override fun deleteMaterial(name: String) {
        var index = 0
        for (material in materials) {
            if (material.name == name) {
                break
            }

            index += 1
        }

        if (index >= materials.size) {
            return
        }

        val material = materials[index]
        renderer.removePipelinesWithMaterial(material)

        material.destroy()
        materials.removeAt(index)
    }

    // Deleting a texture involves removing any material that references it.
    override fun deleteTexture2d(name: String) {
        val texture = textures[name]
        if (texture != null) {
            texture.destroy(logicalDevice)

            val materialsToRemove = ArrayList<String>()
            for (material in materials) {
                for (mt in material.texture2d) {
                    val vt = mt as VulkanTexture2d
                    if (vt.id == texture.id) {
                        materialsToRemove.add(material.name)
                        break
                    }
                }
            }

            for (matName in materialsToRemove) {
                deleteMaterial(matName)
            }

            textures.remove(name)
        }
    }

    override fun getMaterial(name: String): Material {
        for (material in materials) {
            if (material.name == name) {
                return material
            }
        }

        assertion("Material $name does not exist!")
    }

    override fun getTexture2d(name: String): Texture2d {
        return textures[name] ?: assertion("Texture $name does not exist!")
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
