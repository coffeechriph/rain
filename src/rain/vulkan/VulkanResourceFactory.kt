package rain.vulkan

import org.lwjgl.vulkan.VK10
import rain.api.gfx.*
import rain.assertion
import rain.log
import java.nio.ByteBuffer

internal class VulkanResourceFactory(val vk: Vk, val renderer: VulkanRenderer) : ResourceFactory {
    private var resourceId: Long = 0
    private val logicalDevice: LogicalDevice
    private val physicalDevice: PhysicalDevice
    private val queue: Queue
    private val commandPool: CommandPool
    private val materials: MutableList<VulkanMaterial>
    private val textures: MutableMap<String, VulkanTexture2d>
    private val shaders: MutableMap<String, ShaderModule>
    private val buffers: MutableList<VulkanVertexBuffer>
    private val indexBuffers: MutableList<VulkanIndexBuffer>

    init {
        this.materials = ArrayList()
        this.textures = HashMap()
        this.shaders = HashMap()
        this.buffers = ArrayList()
        this.indexBuffers = ArrayList()
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

    override fun createIndexBuffer(indices: IntArray, state: VertexBufferState): IndexBuffer {
        log("Creating index buffer of size ${indices.size*4} bytes.")
        val buffer = VulkanIndexBuffer(uniqueId())
        buffer.create(vk, commandPool, indices, state)
        indexBuffers.add(buffer)
        return buffer
    }

    // TODO: Let's think about if we want to take in a String for the texture instead and load it here...
    override fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d?, depthWriteEnabled: Boolean): Material {
        log("Creating material from sources (vertex: $vertexShaderFile, fragment: $fragmentShaderFile) with texture $texture2d")

        // TODO: We should be able to actually load the shaders at a later time on the main thread
        // In order to make this method thread-safe
        if (!shaders.containsKey(vertexShaderFile)) {
            val vertex = ShaderModule(uniqueId())
            vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
            shaders[vertexShaderFile] = vertex
        }

        if (!shaders.containsKey(fragmentShaderFile)) {
            val fragment = ShaderModule(uniqueId())
            fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
            shaders[fragmentShaderFile] = fragment
        }

        val textures = if (texture2d != null) { Array(1){texture2d!!} } else {Array<Texture2d>(0){VulkanTexture2d(0L)}}
        val material = VulkanMaterial(uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, logicalDevice, physicalDevice.memoryProperties, depthWriteEnabled)
        materials.add(material)
        return material
    }

    override fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, depthWriteEnabled: Boolean): Material {
        log("Creating material from sources (vertex: $vertexShaderFile, fragment: $fragmentShaderFile) with texture $texture2d")

        // TODO: We should be able to actually load the shaders at a later time on the main thread
        // In order to make this method thread-safe
        if (!shaders.containsKey(vertexShaderFile)) {
            val vertex = ShaderModule(uniqueId())
            vertex.loadShader(logicalDevice, vertexShaderFile, VK10.VK_SHADER_STAGE_VERTEX_BIT)
            shaders[vertexShaderFile] = vertex
        }

        if (!shaders.containsKey(fragmentShaderFile)) {
            val fragment = ShaderModule(uniqueId())
            fragment.loadShader(logicalDevice, fragmentShaderFile, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
            shaders[fragmentShaderFile] = fragment
        }

        val textures = if (texture2d.isNotEmpty()) { texture2d } else { Array<Texture2d>(0){VulkanTexture2d(0L)} }
        val material = VulkanMaterial(uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, logicalDevice, physicalDevice.memoryProperties, depthWriteEnabled)
        materials.add(material)

        return material
    }

    // TODO: We should be able to actually load the texture at a later time on the main thread
    // In order to make this method thread-safe
    override fun loadTexture2d(name: String, textureFile: String, filter: TextureFilter): Texture2d {
        if (!textures.containsKey(name)) {
            log("Loading texture $name from $textureFile with filter $filter.")
            val texture2d = VulkanTexture2d(uniqueId())
            texture2d.load(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, textureFile, filter)
            textures[name] = texture2d
        }
        else {
            log("Warning: Texture $name: '$textureFile' is reused based on name!")
        }

        return textures[name]!!
    }

    override fun createTexture2d(name: String, imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter): Texture2d {
        if (!textures.containsKey(name)) {
            log("Creating texture $name from source with filter $filter.")
            val texture2d = VulkanTexture2d(uniqueId())
            texture2d.createImage(logicalDevice, physicalDevice.memoryProperties, commandPool, queue.queue, imageData, width, height, channels, filter)
            textures[name] = texture2d
        }
        else {
            log("Warning: Texture $name: FROM_SOURCE is reused based on name!")
        }

        return textures[name]!!
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

    override fun deleteVertexBuffer(vertexBuffer: VertexBuffer) {
        val vbuf = vertexBuffer as VulkanVertexBuffer
        vbuf.destroy(logicalDevice)
        buffers.remove(vbuf)
    }

    override fun deleteIndexBuffer(indexBuffer: IndexBuffer) {
        val ibuf = indexBuffer as VulkanIndexBuffer
        ibuf.destroy(logicalDevice)
        indexBuffers.remove(ibuf)
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
