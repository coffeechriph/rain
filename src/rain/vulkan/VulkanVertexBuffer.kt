package rain.vulkan

import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.assertion
import rain.log
import java.nio.ByteBuffer

// TODO: Look into updating an existing buffer with new data without recreating any resources
internal class VulkanVertexBuffer(val id: Long, val resourceFactory: VulkanResourceFactory) : VertexBuffer {
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
    private var dataType = DataType.FLOAT

    private lateinit var vk: Vk
    private lateinit var commandBuffer: CommandPool.CommandBuffer

    fun invalidate() {
        isValid = false
    }

    override fun valid(): Boolean {
        return isValid
    }

    // TODO: Can we optimize this?
    override fun update(vertices: ByteBuffer) {
        if (vertices.remaining() <= 0) {
            vertexCount = 0
        }
        else {
            vertexCount = vertices.remaining() / vertexSize / 4
            rawBuffer.buffer(vk.vmaAllocator, vertices)
        }
    }

    fun create(vk: Vk, commandBuffer: CommandPool.CommandBuffer, setupQueue: Queue, vertices: ByteBuffer, attributes: Array<VertexAttribute>, state: VertexBufferState, dataType: DataType) {
        if (vertices.remaining() <= 0) {
            assertion("Unable to create vertex buffer with no vertices!")
        }

        this.dataType = dataType
        rawBuffer = RawBuffer(commandBuffer, setupQueue, resourceFactory)

        if (state == VertexBufferState.STATIC) {
            rawBuffer.create(vk.vmaAllocator, vertices.remaining().toLong(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY)
            rawBuffer.buffer(vk.vmaAllocator, vertices)
        }
        else {
            rawBuffer.create(vk.vmaAllocator, vertices.remaining().toLong(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)
            rawBuffer.buffer(vk.vmaAllocator, vertices)
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

        log("Vertex data size: ${vertices.remaining()}, vertex size: $vertexSize")
        vertexCount = vertices.remaining() / vertexSize / 4
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
            val format = getFormat(dataType, attr.count)
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
