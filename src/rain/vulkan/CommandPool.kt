package rain.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.assertion
import java.nio.LongBuffer

internal class CommandPool {
    var pool: Long = 0
        private set

    fun create(logicalDevice: LogicalDevice, queueFamilyIndex: Int) {
        val cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .queueFamilyIndex(queueFamilyIndex)
                .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        val pCmdPool = memAllocLong(1)
        val err = vkCreateCommandPool(logicalDevice.device, cmdPoolInfo, null, pCmdPool)
        val commandPool = pCmdPool.get(0)
        cmdPoolInfo.free()
        memFree(pCmdPool)

        if (err != VK_SUCCESS) {
            assertion("Failed to create command pool: " + VulkanResult(err))
        }

        pool = commandPool
    }

    fun createCommandBuffer(device: VkDevice, count: Int): Array<CommandBuffer> {
        val cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(pool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(count)

        val pCommandBuffer = memAllocPointer(count)
        val err = vkAllocateCommandBuffers(device, cmdBufAllocateInfo, pCommandBuffer)
        cmdBufAllocateInfo.free()

        val buffers = Array(pCommandBuffer.remaining()) {i -> CommandBuffer(VkCommandBuffer(pCommandBuffer.get(i), device))}
        memFree(pCommandBuffer)
        if (err != VK_SUCCESS) {
            assertion("Failed to allocate command buffer: " + VulkanResult(err))
        }

        return buffers
    }

    class CommandBuffer internal constructor(var buffer: VkCommandBuffer) {
        val commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc()

        private val pCommandBuffer: PointerBuffer
        private val submitInfo: VkSubmitInfo

        init {
            pCommandBuffer = memAllocPointer(1)
            submitInfo = VkSubmitInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pCommandBuffer)
        }

        fun begin() {
            commandBufferBeginInfo
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                    .pNext(0)
            val err = vkBeginCommandBuffer(buffer, commandBufferBeginInfo)
            if (err != VK_SUCCESS) {
                assertion("Failed to begin command buffer: " + VulkanResult(err))
            }
        }

        fun end() {
            val err = vkEndCommandBuffer(buffer)
            if (err != VK_SUCCESS) {
                assertion("Failed to end command buffer: " + VulkanResult(err))
            }
        }

        fun submit(queue: VkQueue) {
            if (buffer.address() == 0L)
                return

            pCommandBuffer.put(buffer)
                    .flip()

            val err = vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE)

            if (err != VK_SUCCESS) {
                assertion("Failed to submit command buffer: " + VulkanResult(err))
            }
        }

        fun submit(queue: Queue, waitSemaphore: Semaphore, signalSemaphore: Semaphore, waitDstStageMask: Int, pFence: LongBuffer) {
            if (buffer.address() == 0L)
                return

            pCommandBuffer.put(buffer)
                    .flip()

            val pWaitSemaphore = memAllocLong(1)
            pWaitSemaphore.put(0, waitSemaphore.semaphore)

            val pSignalSemaphore = memAllocLong(1)
            pSignalSemaphore.put(0, signalSemaphore.semaphore)

            val pWaitDstStageMask = memAllocInt(1)
            pWaitDstStageMask.put(0, waitDstStageMask)

            submitInfo
                    .waitSemaphoreCount(pWaitSemaphore.remaining())
                    .pWaitSemaphores(pWaitSemaphore)
                    .pSignalSemaphores(pSignalSemaphore)
                    .pWaitDstStageMask(pWaitDstStageMask)

            val err = vkQueueSubmit(queue.queue, submitInfo, pFence.get(0))

            if (err != VK_SUCCESS) {
                assertion("Failed to submit command buffer: " + VulkanResult(err))
            }
        }
    }
}
