package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import rain.assertion
import java.nio.ByteBuffer

internal class UniformBuffer {
    lateinit var buffer: LongArray
        private set
    lateinit var bufferMemory: LongArray
        private set
    var bufferSize: Long = 0
        private set
    var mode = BufferMode.SINGLE_BUFFER
        private set

    internal fun create(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, mode: BufferMode, bufferSize: Long) {
        val count = if (mode == BufferMode.SINGLE_BUFFER) {1} else {Swapchain.SWAPCHAIN_MODE.mode}
        this.mode = mode

        this.buffer = LongArray(count)
        this.bufferMemory = LongArray(count)
        this.bufferSize = bufferSize

        for (i in 0 until count) {
            val buf = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
            this.buffer[i] = buf.buffer
            this.bufferMemory[i] = buf.bufferMemory
        }
    }

    internal fun update(logicalDevice: LogicalDevice, bufferData: ByteBuffer, index: Int) {
        val pData = MemoryUtil.memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, bufferMemory[index], 0, bufferSize, 0, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map uniform buffer memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(bufferData), data, bufferData.remaining().toLong())
        vkUnmapMemory(logicalDevice.device, bufferMemory[index])
    }

    fun destroy(logicalDevice: LogicalDevice) {
        for (b in buffer) {
            vkDestroyBuffer(logicalDevice.device, b, null)
        }
    }
}
