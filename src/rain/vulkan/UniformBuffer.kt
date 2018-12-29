package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import rain.assertion
import java.nio.ByteBuffer

internal class UniformBuffer {
    var buffer: Long = 0
        private set
    var bufferMemory: Long = 0
        private set
    var bufferSize: Long = 0
        private set
    var isValid = false
        private set
        get() {
            return buffer == 0L || field
        }

    fun invalidate() {
        isValid = false
    }

    internal fun create(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, bufferSize: Long) {
        this.bufferSize = bufferSize

        val buf = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)
        this.buffer = buf.buffer
        this.bufferMemory = buf.bufferMemory

        isValid = true
    }

    internal fun update(logicalDevice: LogicalDevice, bufferData: ByteBuffer) {
        val pData = MemoryUtil.memAllocPointer(1)
        val err = vkMapMemory(logicalDevice.device, bufferMemory, 0, bufferSize, 0, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map uniform buffer memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(bufferData), data, bufferData.remaining().toLong())
        vkUnmapMemory(logicalDevice.device, bufferMemory)
    }
}
