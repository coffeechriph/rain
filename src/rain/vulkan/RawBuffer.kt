package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkBufferCreateInfo
import rain.assertion
import java.nio.ByteBuffer

internal class RawBuffer(private val setupCommandBuffer: CommandPool.CommandBuffer, private val setupQueue: Queue, private val resourceFactory: VulkanResourceFactory) {
    internal var buffer: Long = 0
        private set
    internal var allocation: Long = 0
        private set

    private var memoryUsage: Int = 0
    private var bufferSize: Long = 0

    internal fun create(vmaAllocator: Long, bufferSize: Long, bufferUsage: Int, bufferMemoryUsage: Int, requiredFlags: Int = 0) {
        if (buffer > 0) {
            resourceFactory.queueRawBufferDeletion(VulkanResourceFactory.DeleteBuffer(buffer, allocation))
            buffer = 0
        }

        val pBufferCreateInfo = VkBufferCreateInfo.calloc()
                .size(bufferSize)
                .usage(bufferUsage)

        val pAllocationCreateInfo = VmaAllocationCreateInfo.calloc()
                .usage(bufferMemoryUsage)
                .requiredFlags(requiredFlags)

        val pBuffer = memAllocLong(1)
        val pAllocation = memAllocPointer(1)
        val err = vmaCreateBuffer(vmaAllocator, pBufferCreateInfo, pAllocationCreateInfo, pBuffer, pAllocation, null)
        if (err != VK_SUCCESS) {
            assertion("VMA Error: ${VulkanResult(err)}")
        }

        buffer = pBuffer[0]
        allocation = pAllocation[0]
        memoryUsage = bufferMemoryUsage
        this.bufferSize = pBufferCreateInfo.size()
    }

    internal fun buffer(vmaAllocator: Long, bufferData: ByteBuffer) {
        val pAllocInfo = VmaAllocationInfo.calloc()
        val pFlags = memAllocInt(1)
        vmaGetAllocationInfo(vmaAllocator, allocation, pAllocInfo)
        vmaGetMemoryTypeProperties(vmaAllocator, pAllocInfo.memoryType(), pFlags)

        if ((pFlags[0] and VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) == VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) {
            mapDataDirect(vmaAllocator, bufferData)
        }
        else {
            mapDataStaging(vmaAllocator, bufferData)
        }
    }

    private fun mapDataDirect(vmaAllocator: Long, bufferData: ByteBuffer) {
        val pData = memAllocPointer(4)
        val err = vmaMapMemory(vmaAllocator, allocation, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map vertex memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(bufferData), data, bufferData.remaining().toLong())
        vmaUnmapMemory(vmaAllocator, allocation)
    }

    private fun mapDataStaging(vmaAllocator: Long, bufferData: ByteBuffer) {
        val stagingBuffer = RawBuffer(setupCommandBuffer, setupQueue, resourceFactory)
        stagingBuffer.create(vmaAllocator, bufferData.remaining().toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VMA_MEMORY_USAGE_CPU_ONLY)
        stagingBuffer.buffer(vmaAllocator, bufferData)

        setupCommandBuffer.begin()
        val copyRegion = VkBufferCopy.calloc(1)
                .srcOffset(0)
                .dstOffset(0)
                .size(bufferSize)
        vkCmdCopyBuffer(setupCommandBuffer.buffer, stagingBuffer.buffer, buffer, copyRegion)

        setupCommandBuffer.end()
        setupCommandBuffer.submit(setupQueue.queue)

        // TODO: Consider fences and a smarter way to build buffers
        vkQueueWaitIdle(setupQueue.queue)

        vmaDestroyBuffer(vmaAllocator, stagingBuffer.buffer, stagingBuffer.allocation)
    }
}
