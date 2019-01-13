package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.Vma.vmaMapMemory
import org.lwjgl.util.vma.Vma.vmaUnmapMemory
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import rain.assertion
import java.nio.ByteBuffer

internal class UniformBuffer(private val vk: Vk, private val setupCommandBuffer: CommandPool.CommandBuffer, private val setupQueue: Queue, private val resourceFactory: VulkanResourceFactory) {
    lateinit var rawBuffer: RawBuffer
    var isValid = false
        private set
        get() {
            return !::rawBuffer.isInitialized || rawBuffer.buffer == 0L || field
        }

    fun invalidate() {
        isValid = false
    }

    internal fun isInitialized(): Boolean {
        return ::rawBuffer.isInitialized
    }

    internal fun create(bufferSize: Long) {
        rawBuffer = RawBuffer(setupCommandBuffer, setupQueue, resourceFactory)
        rawBuffer.create(vk.vmaAllocator, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)
        isValid = true
    }

    internal fun update(bufferData: ByteBuffer) {
        val pData = MemoryUtil.memAllocPointer(1)
        val err = vmaMapMemory(vk.vmaAllocator, rawBuffer.allocation, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK_SUCCESS) {
            assertion("Failed to map uniform buffer memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(bufferData), data, bufferData.remaining().toLong())
        vmaUnmapMemory(vk.vmaAllocator, rawBuffer.allocation)
    }
}
