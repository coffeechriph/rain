package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.VertexBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

// TODO: Look into updating an existing buffer with new data without recreating any resources
internal class VulkanVertexBuffer: VertexBuffer {
    var buffer: Long = 0
        private set
    var bufferSize: Long = 0
        private set
    var vertexCount: Int = 0
        private set

    lateinit var vertexPipelineVertexInputStateCreateInfo: VkPipelineVertexInputStateCreateInfo
        private set

    // TODO: Why are we storing bufferMemory as a LongBuffer
    internal class Buffer(var buffer: Long, var bufferMemory: LongBuffer, var bufferSize: Long)

    fun create(logicalDevice: LogicalDevice, queue: Queue, commandPool: CommandPool, memoryProperties: VkPhysicalDeviceMemoryProperties, vertices: FloatArray, attributes: Array<VertexAttribute>, state: VertexBufferState) {
        if (state == VertexBufferState.STATIC) {
            createVertexBufferWithStaging(logicalDevice, queue, commandPool, memoryProperties, vertices)
        }
        else {
            createVertexBuffer(logicalDevice, queue, commandPool, memoryProperties, vertices)
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
    }

    // TODO: This method is used to create a buffer for image data, which makes
    // TODO: 'VertexBuffer' an ill fit for this class
    internal fun createBuffer(logicalDevice: LogicalDevice, size: Long, usage: Int, properties: Int, memoryProperties: VkPhysicalDeviceMemoryProperties): Buffer {
        val bufInfo = VkBufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .pNext(0)
                .size(size)
                .usage(usage)
                .flags(0)

        val pBuffer = memAllocLong(1)
        var err = vkCreateBuffer(logicalDevice.device, bufInfo, null, pBuffer)

        val buffer = pBuffer.get(0)
        memFree(pBuffer)
        bufInfo.free()
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to create vertex buffer: " + VulkanResult(err))
        }

        val memReqs = VkMemoryRequirements.calloc()
        vkGetBufferMemoryRequirements(logicalDevice.device, buffer, memReqs)

        val memAlloc = VkMemoryAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .pNext(0)
                .allocationSize(memReqs.size())
                .memoryTypeIndex(0)

        val memoryTypeIndex = memAllocInt(1)
        getMemoryType(memoryProperties, memReqs.memoryTypeBits(), properties, memoryTypeIndex)
        memAlloc.memoryTypeIndex(memoryTypeIndex.get(0))

        memFree(memoryTypeIndex)
        memReqs.free()

        val bufferMemory = memAllocLong(1)
        val bufferSize = memAlloc.allocationSize()
        err = vkAllocateMemory(logicalDevice.device, memAlloc, null, bufferMemory)

        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to allocate vertex memory: " + VulkanResult(err))
        }

        err = vkBindBufferMemory(logicalDevice.device, buffer, bufferMemory.get(0), 0)
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to bind memory to vertex buffer: " + VulkanResult(err))
        }

        return Buffer(buffer, bufferMemory, bufferSize)
    }

    private fun createVertexBuffer(logicalDevice: LogicalDevice, queue: Queue, commandPool: CommandPool, memoryProperties: VkPhysicalDeviceMemoryProperties, vertices: FloatArray) {
        val buffer = createBuffer(logicalDevice, (vertices.size * 4).toLong(), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        val vertexBuffer = memAlloc(vertices.size * 4)
        val fb = vertexBuffer.asFloatBuffer()
        fb.put(vertices)

        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, buffer.bufferMemory.get(0), 0, buffer.bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(vertexBuffer), data, vertexBuffer.remaining().toLong())
        memFree(vertexBuffer)
        vkUnmapMemory(logicalDevice.device, buffer.bufferMemory.get(0))

        this.buffer = buffer.buffer
        this.bufferSize = buffer.bufferSize
    }

    private fun createVertexBufferWithStaging(logicalDevice: LogicalDevice, queue: Queue, commandPool: CommandPool, memoryProperties: VkPhysicalDeviceMemoryProperties, vertices: FloatArray) {
        val stagingBuffer = createBuffer(logicalDevice, (vertices.size * 4).toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        val vertexBuffer = memAlloc(vertices.size * 4)
        val fb = vertexBuffer.asFloatBuffer()
        fb.put(vertices)

        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, stagingBuffer.bufferMemory.get(0), 0, stagingBuffer.bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(vertexBuffer), data, vertexBuffer.remaining().toLong())
        memFree(vertexBuffer)
        vkUnmapMemory(logicalDevice.device, stagingBuffer.bufferMemory.get(0))

        val actualBuffer = createBuffer(logicalDevice, stagingBuffer.bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, memoryProperties)
        copyBuffer(logicalDevice, queue, commandPool, stagingBuffer.buffer, actualBuffer.buffer, stagingBuffer.bufferSize)

        vkDestroyBuffer(logicalDevice.device, stagingBuffer.buffer, null)
        vkFreeMemory(logicalDevice.device, stagingBuffer.bufferMemory.get(0), null)

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

// TODO: Move this to another file as it's used by multiple
internal fun getMemoryType(deviceMemoryProperties: VkPhysicalDeviceMemoryProperties, typeBits: Int, properties: Int, typeIndex: IntBuffer): Boolean {
    var bits = typeBits
    for (i in 0..31) {
        if (bits and 1 == 1) {
            if (deviceMemoryProperties.memoryTypes(i).propertyFlags() and properties == properties) {
                typeIndex.put(0, i)
                return true
            }
        }
        bits = bits shr 1
    }
    return false
}