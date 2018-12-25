package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import rain.api.gfx.IndexBuffer
import rain.api.gfx.VertexBufferState
import rain.assertion
import rain.log
import java.nio.ByteBuffer

// TODO: Look into updating an existing buffer with new data without recreating any resources
internal class VulkanIndexBuffer(val id: Long) : IndexBuffer {
    var buffer: Long = 0
        private set
    var indexCount: Int = 0
        private set
    var isValid = false
        private set

    private var bufferState = VertexBufferState.STATIC
    private var bufferSize: Long = 0
    private var bufferMemory: Long = 0

    private lateinit var vk: Vk
    private lateinit var commandPool: CommandPool

    override fun valid(): Boolean {
        return isValid
    }

    override fun update(indices: IntArray) {
        if (indices.isEmpty()) {
            indexCount = 0
        }
        else {
            if (this.buffer > 0L) {
                vkDestroyBuffer(vk.logicalDevice.device, buffer, null)
                this.buffer = 0L
            }

            if (bufferState == VertexBufferState.STATIC) {
                createIndexBufferWithStaging(vk.logicalDevice, vk.deviceQueue, commandPool, vk.physicalDevice.memoryProperties, indices)
            } else {
                if (indices.size >= indexCount*4) {
                    createIndexBuffer(vk.logicalDevice, vk.physicalDevice.memoryProperties, indices)
                }
                else {
                    val bb = memAlloc(indices.size*4)
                    val ib = bb.asIntBuffer()
                    ib.put(indices)
                    mapDataWithoutStaging(vk.logicalDevice, bufferMemory, bufferSize, bb)
                }
            }
        }
    }

    fun destroy(logicalDevice: LogicalDevice) {
        vkDestroyBuffer(logicalDevice.device, buffer, null)
        buffer = 0L
        isValid = false
    }

    fun create(vk: Vk, commandPool: CommandPool, indicies: IntArray, state: VertexBufferState) {
        if (indicies.isEmpty()) {
            assertion("Unable to create vertex buffer with no indicies!")
        }

        if (this.buffer > 0L) {
            vkDestroyBuffer(vk.logicalDevice.device, buffer, null)
            this.buffer = 0L
        }

        if (state == VertexBufferState.STATIC) {
            createIndexBufferWithStaging(vk.logicalDevice, vk.deviceQueue, commandPool, vk.physicalDevice.memoryProperties, indicies)
        }
        else {
            createIndexBuffer(vk.logicalDevice, vk.physicalDevice.memoryProperties, indicies)
        }

        log("Index count: ${indicies.size}")
        indexCount = indicies.size
        this.vk = vk
        this.commandPool = commandPool
        this.isValid = true
    }

    private fun createIndexBuffer(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, indicies: IntArray) {
        val buffer = createBuffer(logicalDevice, (indicies.size * 4).toLong(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or
                VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        val indexBuffer = memAlloc(indicies.size * 4)
        val fb = indexBuffer.asIntBuffer()
        fb.put(indicies)

        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, buffer.bufferMemory, 0, buffer.bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(indexBuffer), data, indexBuffer.remaining().toLong())
        memFree(indexBuffer)
        vkUnmapMemory(logicalDevice.device, buffer.bufferMemory)

        this.buffer = buffer.buffer
        this.bufferSize = buffer.bufferSize
        this.bufferMemory = buffer.bufferMemory
    }

    private fun createIndexBufferWithStaging(logicalDevice: LogicalDevice, queue: Queue, commandPool: CommandPool, memoryProperties: VkPhysicalDeviceMemoryProperties, indicies: IntArray) {
        val stagingBuffer = createBuffer(logicalDevice, (indicies.size * 4).toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        val indexBuffer = memAlloc(indicies.size * 4)
        val fb = indexBuffer.asIntBuffer()
        fb.put(indicies)

        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, stagingBuffer.bufferMemory, 0, stagingBuffer.bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(indexBuffer), data, indexBuffer.remaining().toLong())
        memFree(indexBuffer)
        vkUnmapMemory(logicalDevice.device, stagingBuffer.bufferMemory)

        val actualBuffer = createBuffer(logicalDevice, stagingBuffer.bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, memoryProperties)
        copyBuffer(logicalDevice, queue, commandPool, stagingBuffer.buffer, actualBuffer.buffer, stagingBuffer.bufferSize)

        vkDestroyBuffer(logicalDevice.device, stagingBuffer.buffer, null)
        vkFreeMemory(logicalDevice.device, stagingBuffer.bufferMemory, null)

        this.buffer = actualBuffer.buffer
        this.bufferSize = actualBuffer.bufferSize
        this.bufferMemory = actualBuffer.bufferMemory
    }

    private fun mapDataWithoutStaging(logicalDevice: LogicalDevice, bufferMemory: Long, bufferSize: Long, indexBuffer: ByteBuffer) {
        val pData = memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, bufferMemory, 0, bufferSize, 0, pData)

        val data = pData.get(0)
        memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map vertex memory: " + VulkanResult(err))
        }

        memCopy(memAddress(indexBuffer), data, indexBuffer.remaining().toLong())
        vkUnmapMemory(logicalDevice.device, bufferMemory)
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
}
