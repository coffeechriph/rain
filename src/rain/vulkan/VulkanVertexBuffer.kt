package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.api.assertion

// TODO: Look into updating an existing buffer with new data without recreating any resources
internal class VulkanVertexBuffer: VertexBuffer {
    internal class Buffer(var buffer: Long, var bufferMemory: Long, var bufferSize: Long)

    val id: Long
    var buffer: Long = 0
        private set
    var bufferSize: Long = 0
        private set
    var vertexCount: Int = 0
        private set
    private var vertexSize = 0

    lateinit var vertexPipelineVertexInputStateCreateInfo: VkPipelineVertexInputStateCreateInfo
        private set

    private var bufferState = VertexBufferState.STATIC

    private lateinit var vk: Vk
    private lateinit var commandPool: CommandPool

    constructor(id: Long) {
        this.id = id
    }

    // TODO: Can we optimize this?
    override fun update(vertices: FloatArray) {
        if (vertices.isEmpty()) {
            vertexCount = 0
        }
        else {
            vertexCount = vertices.size / vertexSize
            if (this.buffer > 0L) {
                vkDestroyBuffer(vk.logicalDevice.device, buffer, null)
                this.buffer = 0L
            }

            if (bufferState == VertexBufferState.STATIC) {
                createVertexBufferWithStaging(vk.logicalDevice, vk.deviceQueue, commandPool, vk.physicalDevice.memoryProperties, vertices)
            } else {
                createVertexBuffer(vk.logicalDevice, vk.physicalDevice.memoryProperties, vertices)
            }
        }
    }

    fun destroy(logicalDevice: LogicalDevice) {
        vkDestroyBuffer(logicalDevice.device, buffer, null)
        buffer = 0L
    }

    fun create(vk: Vk, commandPool: CommandPool, vertices: FloatArray, attributes: Array<VertexAttribute>, state: VertexBufferState) {
        if (vertices.isEmpty()) {
            assertion("Unable to create vertex buffer with no vertices!")
        }

        if (this.buffer > 0L) {
            vkDestroyBuffer(vk.logicalDevice.device, buffer, null)
            this.buffer = 0L
        }

        if (state == VertexBufferState.STATIC) {
            createVertexBufferWithStaging(vk.logicalDevice, vk.deviceQueue, commandPool, vk.physicalDevice.memoryProperties, vertices)
        }
        else {
            createVertexBuffer(vk.logicalDevice, vk.physicalDevice.memoryProperties, vertices)
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

        vertexCount = vertices.size / vertexSize
        this.vertexSize = vertexSize
        this.vk = vk
        this.commandPool = commandPool
    }

    private fun createVertexBuffer(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, vertices: FloatArray) {
        val buffer = createBuffer(logicalDevice, (vertices.size * 4).toLong(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        val vertexBuffer = memAlloc(vertices.size * 4)
        val fb = vertexBuffer.asFloatBuffer()
        fb.put(vertices)

        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, buffer.bufferMemory, 0, buffer.bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(vertexBuffer), data, vertexBuffer.remaining().toLong())
        memFree(vertexBuffer)
        vkUnmapMemory(logicalDevice.device, buffer.bufferMemory)

        this.buffer = buffer.buffer
        this.bufferSize = buffer.bufferSize
    }

    private fun createVertexBufferWithStaging(logicalDevice: LogicalDevice, queue: Queue, commandPool: CommandPool, memoryProperties: VkPhysicalDeviceMemoryProperties, vertices: FloatArray) {
        val stagingBuffer = createBuffer(logicalDevice, (vertices.size * 4).toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        val vertexBuffer = memAlloc(vertices.size * 4)
        val fb = vertexBuffer.asFloatBuffer()
        fb.put(vertices)

        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, stagingBuffer.bufferMemory, 0, stagingBuffer.bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(vertexBuffer), data, vertexBuffer.remaining().toLong())
        memFree(vertexBuffer)
        vkUnmapMemory(logicalDevice.device, stagingBuffer.bufferMemory)

        val actualBuffer = createBuffer(logicalDevice, stagingBuffer.bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, memoryProperties)
        copyBuffer(logicalDevice, queue, commandPool, stagingBuffer.buffer, actualBuffer.buffer, stagingBuffer.bufferSize)

        vkDestroyBuffer(logicalDevice.device, stagingBuffer.buffer, null)
        vkFreeMemory(logicalDevice.device, stagingBuffer.bufferMemory, null)

        this.buffer = actualBuffer.buffer
        this.bufferSize = actualBuffer.bufferSize
    }

    private fun copyBuffer(logicalDevice: LogicalDevice, queue: Queue, commandPool: CommandPool, srcBuffer: Long, dstBuffer: Long, bufferSize: Long) {
        val commandBuffer = commandPool.createCommandBuffer(logicalDevice.device, 1)[0]
        commandBuffer.begin()
        val copyRegion = VkBufferCopy.calloc(1)
                .srcOffset(0)
                .dstOffset(0)
                .size(bufferSize)
        vkCmdCopyBuffer(commandBuffer.buffer, srcBuffer, dstBuffer, copyRegion)

        commandBuffer.end()
        commandBuffer.submit(queue.queue)

        // TODO: Consider fences and a smarter way to build buffers
        vkQueueWaitIdle(queue.queue)
        vkFreeCommandBuffers(logicalDevice.device, commandPool.pool, commandBuffer.buffer)
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
            val format = when(attr.count) {
                1 -> VK_FORMAT_R32_SFLOAT
                2 -> VK_FORMAT_R32G32_SFLOAT
                3 -> VK_FORMAT_R32G32B32_SFLOAT
                4 -> VK_FORMAT_R32G32B32A32_SFLOAT
                else -> throw IllegalArgumentException("Unsupported number of components for vertex attribute: ${attr.count}")
            }

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
}
