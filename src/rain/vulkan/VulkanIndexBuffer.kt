package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.util.vma.Vma
import org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT
import rain.api.gfx.IndexBuffer
import rain.api.gfx.VertexBufferState
import rain.assertion
import rain.log

// TODO: Look into updating an existing buffer with new data without recreating any resources
internal class VulkanIndexBuffer(val id: Long, val resourceFactory: VulkanResourceFactory) : IndexBuffer {
    lateinit var rawBuffer: RawBuffer
    var indexCount: Int = 0
        private set
    var isValid = false
        private set

    private var bufferState = VertexBufferState.STATIC
    private lateinit var vk: Vk

    fun invalidate() {
        isValid = false
    }

    override fun valid(): Boolean {
        return isValid
    }

    override fun update(indices: IntArray) {
        if (indices.isEmpty()) {
            indexCount = 0
        }
        else {
            val dataBuffer = memAlloc(indices.size * 4)
            dataBuffer.asIntBuffer().put(indices).flip()

            if (bufferState == VertexBufferState.STATIC) {
                rawBuffer.create(vk.vmaAllocator, dataBuffer.remaining().toLong(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY)
                rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
            } else {
                if (indices.size >= indexCount) {
                    rawBuffer.create(vk.vmaAllocator, dataBuffer.remaining().toLong(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)
                }

                rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
            }
        }
    }

    fun create(vk: Vk, setupCommandBuffer: CommandPool.CommandBuffer, setupQueue: Queue, indicies: IntArray, state: VertexBufferState) {
        if (indicies.isEmpty()) {
            assertion("Unable to create vertex buffer with no indicies!")
        }

        val dataBuffer = memAlloc(indicies.size*4)
        dataBuffer.asIntBuffer().put(indicies).flip()

        rawBuffer = RawBuffer(setupCommandBuffer, setupQueue, resourceFactory)

        if (state == VertexBufferState.STATIC) {
            rawBuffer.create(vk.vmaAllocator, dataBuffer.remaining().toLong(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_GPU_ONLY)
            rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
        }
        else {
            rawBuffer.create(vk.vmaAllocator, dataBuffer.remaining().toLong(), VK_BUFFER_USAGE_INDEX_BUFFER_BIT, Vma.VMA_MEMORY_USAGE_CPU_TO_GPU)
            rawBuffer.buffer(vk.vmaAllocator, dataBuffer)
        }

        log("Index count: ${indicies.size}")
        indexCount = indicies.size
        this.vk = vk
        this.isValid = true
    }
}
