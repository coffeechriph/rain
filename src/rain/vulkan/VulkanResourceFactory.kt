package rain.vulkan

import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import rain.api.gfx.*
import rain.assertion
import rain.log
import java.nio.ByteBuffer
import java.util.*

internal class VulkanResourceFactory(private val vk: Vk) : ResourceFactory {
    data class DeleteBuffer(val buffer: Long, val allocation: Long)

    private var resourceId: Long = 0
    private val logicalDevice: LogicalDevice
    private val physicalDevice: PhysicalDevice
    private val setupQueue: Queue
    private val commandPool: CommandPool
    private val setupCommandBuffer: CommandPool.CommandBuffer
    private val materials: MutableList<VulkanMaterial>
    private val textures: MutableMap<String, VulkanTexture2d>
    private val shaders: MutableMap<String, ShaderModule>
    private val buffers: MutableList<VulkanVertexBuffer>
    private val indexBuffers: MutableList<VulkanIndexBuffer>

    // TODO: Bug!
    // TODO: We delete whatever the references of these resources are using
    // This will cause issues when we queue them for deletion and realloc them as the newly allocated
    // stuff will be deleted...
    private val deleteMaterialQueue = ArrayDeque<VulkanMaterial>()
    private val deleteTextureQueue = ArrayDeque<VulkanTexture2d>()
    private val deleteShaderQueue = ArrayDeque<ShaderModule>()
    private val deleteRawBufferQueue = ArrayDeque<DeleteBuffer>()

    init {
        this.materials = ArrayList()
        this.textures = HashMap()
        this.shaders = HashMap()
        this.buffers = ArrayList()
        this.indexBuffers = ArrayList()
        this.logicalDevice = vk.logicalDevice
        this.physicalDevice = vk.physicalDevice
        this.setupQueue = vk.deviceQueue
        this.commandPool = CommandPool()
        this.commandPool.create(logicalDevice, vk.queueFamilyIndices.graphicsFamily)
        this.setupCommandBuffer = commandPool.createCommandBuffer(logicalDevice.device, 1)[0]
    }

    fun queueRawBufferDeletion(buffer: DeleteBuffer) {
        deleteRawBufferQueue.add(buffer)
    }

    override fun createVertexBuffer(vertices: FloatArray, state: VertexBufferState, attributes: Array<VertexAttribute>): VulkanVertexBuffer {
        log("Creating vertex buffer of size ${vertices.size * 4} bytes.")
        val buffer = VulkanVertexBuffer(uniqueId(), this)
        buffer.create(vk, setupCommandBuffer, setupQueue, vertices, attributes, state)
        buffers.add(buffer)
        return buffer
    }

    override fun createIndexBuffer(indices: IntArray, state: VertexBufferState): IndexBuffer {
        log("Creating index buffer of size ${indices.size*4} bytes.")
        val buffer = VulkanIndexBuffer(uniqueId(), this)
        buffer.create(vk, setupCommandBuffer, setupQueue, indices, state)
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

        val textures = if (texture2d != null) { Array(1){texture2d!!} } else {Array<Texture2d>(0){VulkanTexture2d(0L, vk, this)}}
        val material = VulkanMaterial(vk, setupCommandBuffer, setupQueue, this, logicalDevice, uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, depthWriteEnabled)
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

        val textures = if (texture2d.isNotEmpty()) { texture2d } else { Array<Texture2d>(0){VulkanTexture2d(0L, vk, this)} }
        val material = VulkanMaterial(vk, setupCommandBuffer, setupQueue, this, logicalDevice, uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, depthWriteEnabled)
        materials.add(material)

        return material
    }

    // TODO: We should be able to actually load the texture at a later time on the main thread
    // In order to make this method thread-safe
    override fun loadTexture2d(name: String, textureFile: String, filter: TextureFilter): Texture2d {
        if (!textures.containsKey(name)) {
            log("Loading texture $name from $textureFile with filter $filter.")
            val texture2d = VulkanTexture2d(uniqueId(), vk, this)
            texture2d.load(logicalDevice, physicalDevice.memoryProperties, setupCommandBuffer, setupQueue, textureFile, filter)
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
            val texture2d = VulkanTexture2d(uniqueId(), vk, this)
            texture2d.createImage(logicalDevice, physicalDevice.memoryProperties, setupCommandBuffer, setupQueue, imageData, width, height, channels, filter)
            textures[name] = texture2d
        }
        else {
            log("Warning: Texture $name: FROM_SOURCE is reused based on name!")
        }

        return textures[name]!!
    }

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
        material.invalidate()
        deleteRawBufferQueue.add(DeleteBuffer(material.sceneData.rawBuffer.buffer, material.sceneData.rawBuffer.allocation))
        deleteRawBufferQueue.add(DeleteBuffer(material.textureDataUBO.rawBuffer.buffer, material.textureDataUBO.rawBuffer.allocation))
        deleteMaterialQueue.add(material)
    }

    override fun deleteTexture2d(name: String) {
        val texture = textures[name]
        if (texture != null) {
            texture.invalidate()
            deleteTextureQueue.add(texture)
        }
    }

    override fun deleteVertexBuffer(vertexBuffer: VertexBuffer) {
        val vbuf = vertexBuffer as VulkanVertexBuffer
        deleteRawBufferQueue.add(DeleteBuffer(vbuf.rawBuffer.buffer, vbuf.rawBuffer.allocation))
    }

    override fun deleteIndexBuffer(indexBuffer: IndexBuffer) {
        val ibuf = indexBuffer as VulkanIndexBuffer
        ibuf.invalidate()
        deleteRawBufferQueue.add(DeleteBuffer(ibuf.rawBuffer.buffer, ibuf.rawBuffer.allocation))
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
            material.invalidate()
            deleteRawBufferQueue.add(DeleteBuffer(material.sceneData.rawBuffer.buffer, material.sceneData.rawBuffer.allocation))

            if (material.textureDataUBO.isInitialized()) {
                deleteRawBufferQueue.add(DeleteBuffer(material.textureDataUBO.rawBuffer.buffer, material.textureDataUBO.rawBuffer.allocation))
            }

            deleteMaterialQueue.add(material)
        }
        materials.clear()

        for (texture in textures) {
            deleteTextureQueue.add(texture.value)
        }
        textures.clear()

        for (shader in shaders) {
            deleteShaderQueue.add(shader.value)
        }
        shaders.clear()

        for (buffer in buffers) {
            buffer.invalidate()
            deleteRawBufferQueue.add(DeleteBuffer(buffer.rawBuffer.buffer, buffer.rawBuffer.allocation))
        }
        buffers.clear()

        for (buffer in indexBuffers) {
            buffer.invalidate()
            deleteRawBufferQueue.add(DeleteBuffer(buffer.rawBuffer.buffer, buffer.rawBuffer.allocation))
        }
        indexBuffers.clear()
    }

    fun manageResources() {
        if (deleteTextureQueue.isNotEmpty() || deleteMaterialQueue.isNotEmpty() || deleteShaderQueue.isNotEmpty() || deleteRawBufferQueue.isNotEmpty()) {
            vkDeviceWaitIdle(vk.logicalDevice.device)

            while (deleteTextureQueue.isNotEmpty()) {
                val texture = deleteTextureQueue.pop()
                VK10.vkDestroyImage(logicalDevice.device, texture.texture, null)
                VK10.vkDestroySampler(logicalDevice.device, texture.textureSampler, null)
                texture.invalidate()
                textures.values.remove(texture)
            }

            while (deleteShaderQueue.isNotEmpty()) {
                val shader = deleteShaderQueue.pop()
                VK10.vkDestroyShaderModule(logicalDevice.device, shader.moduleId, null)
                shader.invalidate()
                shaders.values.remove(shader)
            }

            while (deleteMaterialQueue.isNotEmpty()) {
                val material = deleteMaterialQueue.pop()

                //VK10.vkDestroyDescriptorPool(logicalDevice.device, material.descriptorPool.pool, null)
                material.descriptorPool.invalidate()

                material.invalidate()
                materials.remove(material)
            }

            while (deleteRawBufferQueue.isNotEmpty()) {
                val buffer = deleteRawBufferQueue.pop()
                VK10.vkDestroyBuffer(logicalDevice.device, buffer.buffer, null)
                Vma.vmaFreeMemory(vk.vmaAllocator, buffer.allocation)
            }

            vkDeviceWaitIdle(vk.logicalDevice.device)
        }
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
