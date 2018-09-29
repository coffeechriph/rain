package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import java.nio.ByteBuffer

internal class UniformBuffer {
    lateinit var buffer: LongArray
    lateinit var bufferMemory: LongArray
    var bufferSize: Long = 0

    internal fun create(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, count: Int, bufferSize: Long) {
        this.buffer = LongArray(count)
        this.bufferMemory = LongArray(count)
        this.bufferSize = bufferSize

        for (i in 0 until count) {
            val buf = VertexBuffer().createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
            this.buffer[i] = buf.buffer
            this.bufferMemory[i] = buf.bufferMemory.get(0)
        }
    }

    internal fun update(logicalDevice: LogicalDevice, bufferData: ByteBuffer, index: Int) {
        val pData = MemoryUtil.memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, bufferMemory[index], 0, bufferSize, 0, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to map uniform buffer memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(bufferData), data, bufferData.remaining().toLong())
        vkUnmapMemory(logicalDevice.device, bufferMemory[index])
    }
}