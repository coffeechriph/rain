package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.assertion
import rain.log
import java.nio.ByteBuffer

// TODO: Look into updating an existing buffer with new data without recreating any resources
internal class VulkanVertexBuffer(val id: Long, val resourceFactory: VulkanResourceFactory) : VertexBuffer {
    internal class Buffer(var buffer: Long, var bufferMemory: Long, var bufferSize: Long)
    var isValid = false
        private set
    var vertexCount: Int = 0
        private set
    lateinit var attributes: Array<VertexAttribute>
        private set

    lateinit var vertexPipelineVertexInputStateCreateInfo: VkPipelineVertexInputStateCreateInfo
        private set

    lateinit var rawBuffer: RawBuffer
    private var vertexSize = 0
    private var bufferState = VertexBufferState.STATIC

    private lateinit var vk: Vk
    private lateinit var commandBuffer: CommandPool.CommandBuffer

    private lateinit var dataBuffer: ByteBuffer

    fun invalidate() {
        isValid = false
    }

    override fun valid(): Boolean {
        return isValid
    }

    // TODO: Can we optimize this?
    override fun update(vertices: FloatArray) {
        if (vertices.isEmpty()) {
            vertexCount = 0
        }
        else {
            dataBuffer = memAlloc(vertices.size*4)
            dataBuffer.asFloatBuffer().put(vertices).flip()

            vertexCount = vertices.size / vertexSize
            if (bufferState == VertexBufferState.STATIC) {
                rawBuffer.create(vk.vmaAllocator, dataBuffer, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY)
                rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
            } else {
                if (vertices.size > dataBuffer.capacity()/4) {
                    rawBuffer.create(vk.vmaAllocator, dataBuffer, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)
                }
                rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
            }
        }
    }

    fun create(vk: Vk, commandBuffer: CommandPool.CommandBuffer, vertices: FloatArray, attributes: Array<VertexAttribute>, state: VertexBufferState) {
        if (vertices.isEmpty()) {
            assertion("Unable to create vertex buffer with no vertices!")
        }

        dataBuffer = memAlloc(vertices.size*4)
        dataBuffer.asFloatBuffer().put(vertices).flip()

        rawBuffer = RawBuffer(commandBuffer, vk.deviceQueue, resourceFactory)

        if (state == VertexBufferState.STATIC) {
            rawBuffer.create(vk.vmaAllocator, dataBuffer, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY)
            rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
        }
        else {
            rawBuffer.create(vk.vmaAllocator, dataBuffer, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)
            rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
        }

        // Assign to vertex buffer
        vertexPipelineVertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pNext(0)
                .pVertexBindingDescriptions(createBindingDescription(attributes))
                .pVertexAttributeDescriptions(createAttributeDescription(attributes))

        // TODO: We now officially loop through attributes 3 times...
        var vertexSize = 0
        for (attribute in attributes) {
            vertexSize += attribute.count
        }

        log("Vertex count: ${vertices.size}, vertex size: $vertexSize")
        vertexCount = vertices.size / vertexSize
        this.vertexSize = vertexSize
        this.vk = vk
        this.commandBuffer = commandBuffer
        this.bufferState = state
        this.attributes = attributes
        this.isValid = true
    }

    // TODO: Can only bind to one point at this point in time
    private fun createBindingDescription(attributes: Array<VertexAttribute>): VkVertexInputBindingDescription.Buffer {
        var stride = 0
        for (attribute in attributes) {
            stride += 4 * attribute.count
        }

        return VkVertexInputBindingDescription.calloc(1)
                .binding(0) // <- we bind our vertex buffer to point 0
                .stride(stride)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
    }

    private fun createAttributeDescription(attributes: Array<VertexAttribute>): VkVertexInputAttributeDescription.Buffer {
        val desc = VkVertexInputAttributeDescription.calloc(attributes.size)

        var index = 0
        var lastCount = 0
        for (attr in attributes) {
            val format = getFormat(attr.dataType, attr.count)
            desc.get(index)
                    .binding(0)
                    .location(attr.location)
                    .format(format)
                    .offset(index*lastCount*4)

            index += 1
            lastCount = attr.count
        }

        return desc
    }

    private fun getFormat(dataType: DataType, count: Int): Int {
        if (dataType == DataType.FLOAT) {
            return when(count) {
                1 -> VK_FORMAT_R32_SFLOAT
                2 -> VK_FORMAT_R32G32_SFLOAT
                3 -> VK_FORMAT_R32G32B32_SFLOAT
                4 -> VK_FORMAT_R32G32B32A32_SFLOAT
                else -> throw IllegalArgumentException("Unsupported number of components for vertex attribute: ${count}")
            }
        }
        else if (dataType == DataType.INT) {
            return when(count) {
                1 -> VK_FORMAT_R32_SINT
                2 -> VK_FORMAT_R32G32_SINT
                3 -> VK_FORMAT_R32G32B32_SINT
                4 -> VK_FORMAT_R32G32B32A32_SINT
                else -> throw IllegalArgumentException("Unsupported number of components for vertex attribute: ${count}")
            }
        }

        assertion("Data type $dataType not supported!")
    }
}
