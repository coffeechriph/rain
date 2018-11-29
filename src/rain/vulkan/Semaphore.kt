package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import rain.assertion

internal class Semaphore {
    var semaphore: Long = 0
        private set

    fun create(logicalDevice: LogicalDevice) {
        // TODO: We don't have to create this everytime
        val semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                .pNext(0)
                .flags(0)

        val sem = memAllocLong(1)

        val err = vkCreateSemaphore(logicalDevice.device, semaphoreCreateInfo, null, sem)
        if (err != VK_SUCCESS) {
            assertion("Failed to create semaphore " + VulkanResult(err))
        }

        semaphore = sem.get(0)
    }
}
