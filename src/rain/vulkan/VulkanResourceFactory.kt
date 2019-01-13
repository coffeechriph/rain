package rain.vulkan

import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.vkDestroyBufferView
import org.lwjgl.vulkan.VK10.vkDeviceWaitIdle
import rain.api.gfx.*
import rain.assertion
import rain.log
import java.nio.ByteBuffer
import java.util.*

internal class VulkanResourceFactory(private val vk: Vk) : ResourceFactory {
    data class DeleteBuffer(val buffer: Long, val allocation: Long)
    data class DeleteTexture(val image: Long, val allocation: Long, val imageView: Long, val sampler: Long)
    data class DeleteMaterial(val pool: Long)
    data class DeleteBufferView(val bufferView: Long)

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
    private val deleteMaterialQueue = ArrayDeque<DeleteMaterial>()
    private val deleteTextureQueue = ArrayDeque<DeleteTexture>()
    private val deleteShaderQueue = ArrayDeque<Long>()
    private val deleteRawBufferQueue = ArrayDeque<DeleteBuffer>()
    private val deleteBufferView = ArrayDeque<DeleteBufferView>()

    private val materialBuilder = MaterialBuilder(this::createMaterial)

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

    internal fun queueRawBufferDeletion(buffer: DeleteBuffer) {
        deleteRawBufferQueue.add(buffer)
    }

    internal fun queueBufferViewDeletion(view: DeleteBufferView) {
        deleteBufferView.add(view)
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

    override fun buildMaterial(): MaterialBuilder {
        return materialBuilder
    }

    internal fun createMaterial(name: String, vertex: ShaderModule, fragment: ShaderModule, texture2d: Texture2d?, useBatching: Boolean = false, depthWriteEnabled: Boolean = true, enableBlend: Boolean = true, srcColor: BlendMode = BlendMode.BLEND_FACTOR_SRC_ALPHA, dstColor: BlendMode = BlendMode.BLEND_FACTOR_ONE_MINUS_SRC_ALPHA, srcAlpha: BlendMode = BlendMode.BLEND_FACTOR_ONE, dstAlpha: BlendMode = BlendMode.BLEND_FACTOR_ZERO): Material {
        val textures = if (texture2d != null) { Array(1){texture2d!!} } else {Array<Texture2d>(0){VulkanTexture2d(0L, vk, this)}}
        val material = VulkanMaterial(vk, setupCommandBuffer, setupQueue, this, uniqueId(), name, vertex, fragment, textures, depthWriteEnabled, enableBlend, srcColor, dstColor, srcAlpha, dstAlpha)
        material.batching = useBatching
        materials.add(material)
        return material
    }

    private fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d?, useBatching: Boolean, depthWriteEnabled: Boolean, enableBlend: Boolean, srcColor: BlendMode, dstColor: BlendMode, srcAlpha: BlendMode, dstAlpha: BlendMode): Material {
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
        val material = VulkanMaterial(vk, setupCommandBuffer, setupQueue, this, uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, depthWriteEnabled, enableBlend, srcColor, dstColor, srcAlpha, dstAlpha)
        material.batching = useBatching

        if (useBatching) {
            material.texelBufferUniform = UniformTexelBuffer(vk, setupCommandBuffer, setupQueue, this)
            material.texelBufferUniform.create(256)
            material.descriptorPool.withUniformTexelBuffer(material.texelBufferUniform, VK10.VK_SHADER_STAGE_ALL)
            material.descriptorPool.build(vk.logicalDevice)
        }

        materials.add(material)
        return material
    }

    // TODO: Let's think about if we want to take in a String for the texture instead and load it here...
    /*override fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d?, useBatching: Boolean, depthWriteEnabled: Boolean, enableBlend: Boolean, srcColor: BlendMode, dstColor: BlendMode, srcAlpha: BlendMode, dstAlpha: BlendMode): Material {
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
        val material = VulkanMaterial(vk, setupCommandBuffer, setupQueue, this, uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, depthWriteEnabled, enableBlend, srcColor, dstColor, srcAlpha, dstAlpha)
        material.batching = useBatching
        materials.add(material)
        return material
    }*/

    override fun createTexelBuffer(initialSize: Long): TexelBuffer {
        val texelBuffer = UniformTexelBuffer(vk, setupCommandBuffer, setupQueue, this)
        texelBuffer.create(initialSize)
        return texelBuffer
    }

    /*override fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, depthWriteEnabled: Boolean, enableBlend: Boolean, srcColor: BlendMode, dstColor: BlendMode, srcAlpha: BlendMode, dstAlpha: BlendMode): Material {
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
        val material = VulkanMaterial(vk, setupCommandBuffer, setupQueue, this, uniqueId(), name, shaders[vertexShaderFile]!!, shaders[fragmentShaderFile]!!, textures, depthWriteEnabled, enableBlend, srcColor, dstColor, srcAlpha, dstAlpha)
        materials.add(material)
        return material
    }*/

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
        deleteMaterialQueue.add(DeleteMaterial(material.descriptorPool.pool))
        materials.remove(material)
    }

    override fun deleteTexture2d(name: String) {
        val texture = textures[name]
        if (texture != null) {
            texture.invalidate()
            deleteTextureQueue.add(DeleteTexture(texture.texture, texture.allocation, texture.textureView, texture.textureSampler))
            textures.remove(name)
        }
    }

    override fun deleteVertexBuffer(vertexBuffer: VertexBuffer) {
        val vbuf = vertexBuffer as VulkanVertexBuffer
        vbuf.invalidate()
        deleteRawBufferQueue.add(DeleteBuffer(vbuf.rawBuffer.buffer, vbuf.rawBuffer.allocation))
        buffers.remove(vbuf)
    }

    override fun deleteIndexBuffer(indexBuffer: IndexBuffer) {
        val ibuf = indexBuffer as VulkanIndexBuffer
        ibuf.invalidate()
        deleteRawBufferQueue.add(DeleteBuffer(ibuf.rawBuffer.buffer, ibuf.rawBuffer.allocation))
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
            material.invalidate()
            deleteRawBufferQueue.add(DeleteBuffer(material.sceneData.rawBuffer.buffer, material.sceneData.rawBuffer.allocation))

            if (material.textureDataUBO.isInitialized()) {
                deleteRawBufferQueue.add(DeleteBuffer(material.textureDataUBO.rawBuffer.buffer, material.textureDataUBO.rawBuffer.allocation))
            }

            deleteMaterialQueue.add(DeleteMaterial(material.descriptorPool.pool))
        }
        materials.clear()

        for (texture in textures) {
            val tex = texture.value
            deleteTextureQueue.add(DeleteTexture(tex.texture, tex.allocation, tex.textureView, tex.textureSampler))
            tex.invalidate()
        }
        textures.clear()

        for (shader in shaders) {
            deleteShaderQueue.add(shader.value.moduleId)
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
                Vma.vmaDestroyImage(vk.vmaAllocator, texture.image, texture.allocation)
                VK10.vkDestroySampler(logicalDevice.device, texture.sampler, null)
                VK10.vkDestroyImageView(logicalDevice.device, texture.imageView, null)
            }

            while (deleteShaderQueue.isNotEmpty()) {
                val shader = deleteShaderQueue.pop()
                VK10.vkDestroyShaderModule(logicalDevice.device, shader, null)
            }

            // TODO: Why can't we destroy the descriptor pool??
            while (deleteMaterialQueue.isNotEmpty()) {
                val material = deleteMaterialQueue.pop()
                //VK10.vkDestroyDescriptorPool(logicalDevice.device, material.pool, null)
            }

            while (deleteRawBufferQueue.isNotEmpty()) {
                val buffer = deleteRawBufferQueue.pop()
                VK10.vkDestroyBuffer(logicalDevice.device, buffer.buffer, null)
                Vma.vmaFreeMemory(vk.vmaAllocator, buffer.allocation)
            }

            while (deleteBufferView.isNotEmpty()) {
                val view = deleteBufferView.pop()
                vkDestroyBufferView(logicalDevice.device, view.bufferView, null)
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
