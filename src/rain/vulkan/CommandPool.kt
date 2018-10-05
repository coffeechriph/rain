package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

internal class CommandPool {
    var pool: Long = 0
        private set

    fun create(logicalDevice: LogicalDevice, queueFamilyIndex: Int) {
        val cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .queueFamilyIndex(queueFamilyIndex)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        val pCmdPool = memAllocLong(1)
        val err = vkCreateCommandPool(logicalDevice.device, cmdPoolInfo, null, pCmdPool)
        val commandPool = pCmdPool.get(0)
        cmdPoolInfo.free()
        memFree(pCmdPool)

        if (err != VK_SUCCESS) {
            throw AssertionError("Failed to create command pool: " + VulkanResult(err))
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
            throw AssertionError("Failed to allocate command buffer: " + VulkanResult(err))
        }

        return buffers
    }

    class CommandBuffer internal constructor(var buffer: VkCommandBuffer) {
        val commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc()

        fun begin() {
            commandBufferBeginInfo
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                    .pNext(0)
            val err = vkBeginCommandBuffer(buffer, commandBufferBeginInfo)
            if (err != VK_SUCCESS) {
                throw AssertionError("Failed to begin command buffer: " + VulkanResult(err))
            }
        }

        fun end() {
            val err = vkEndCommandBuffer(buffer)
            if (err != VK_SUCCESS) {
                throw AssertionError("Failed to end command buffer: " + VulkanResult(err))
            }
        }

        fun submit(queue: VkQueue) {
            if (buffer.address() == 0L)
                return

            // TODO: Don't have to allocate this every time
            val pCommandBuffers = memAllocPointer(1)
                    .put(buffer)
                    .flip()

            val submitInfo = VkSubmitInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pCommandBuffers)

            val err = vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE)
            memFree(pCommandBuffers)
            submitInfo.free()

            if (err != VK_SUCCESS) {
                throw AssertionError("Failed to submit command buffer: " + VulkanResult(err))
            }
        }

        // TODO: Accept a Queue instead of VkQueue
        fun submit(queue: VkQueue, waitSemaphore: Semaphore, signalSemaphore: Semaphore, waitDstStageMask: Int) {
            if (buffer.address() == 0L)
                return

            // TODO: Don't have to allocate this every time
            val pCommandBuffers = memAllocPointer(1)
                    .put(buffer)
                    .flip()

            val pWaitSemaphore = memAllocLong(1)
            pWaitSemaphore.put(0, waitSemaphore.semaphore)

            val pSignalSemaphore = memAllocLong(1)
            pSignalSemaphore.put(0, signalSemaphore.semaphore)

            val pWaitDstStageMask = memAllocInt(1)
            pWaitDstStageMask.put(0, waitDstStageMask)

            val submitInfo = VkSubmitInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(pCommandBuffers)
                    .waitSemaphoreCount(pWaitSemaphore.remaining())
                    .pWaitSemaphores(pWaitSemaphore)
                    .pSignalSemaphores(pSignalSemaphore)
                    .pWaitDstStageMask(pWaitDstStageMask)

            val err = vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE)
            memFree(pCommandBuffers)
            submitInfo.free()

            if (err != VK_SUCCESS) {
                throw AssertionError("Failed to submit command buffer: " + VulkanResult(err))
            }
        }
    }
}
