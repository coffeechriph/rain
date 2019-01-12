package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferViewCreateInfo
import rain.api.gfx.TexelBuffer
import rain.assertion
import java.nio.ByteBuffer

internal class UniformTexelBuffer(private val vk: Vk, private val setupCommandBuffer: CommandPool.CommandBuffer, private val setupQueue: Queue, private val resourceFactory: VulkanResourceFactory): TexelBuffer {
    lateinit var rawBuffer: RawBuffer
    var bufferView: Long = 0
        private set

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
        rawBuffer.create(vk.vmaAllocator, bufferSize, VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)

        // TODO: Allow client to specify format
        val pBufferViewCreateInfo = VkBufferViewCreateInfo.calloc()
                .buffer(rawBuffer.buffer)
                .format(VK_FORMAT_R32_SFLOAT)
                .offset(0)
                .range(VK_WHOLE_SIZE)

        val pBufferView = memAllocLong(1)
        val err = vkCreateBufferView(vk.logicalDevice.device, pBufferViewCreateInfo, null, pBufferView)
        if (err != VK_SUCCESS) {
            assertion("Error creating buffer view: ${VulkanResult(err)}")
        }

        bufferView = pBufferView[0]
        isValid = true
    }

    override fun update(data: ByteBuffer) {
        if (::rawBuffer.isInitialized) {
            rawBuffer.buffer(vk.vmaAllocator, data)
        }
    }
}
