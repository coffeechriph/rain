package rain.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.vkGetDeviceQueue
import org.lwjgl.vulkan.VkQueue

internal class Queue {
    lateinit var queue: VkQueue

    fun create(logicalDevice: LogicalDevice, queueIndex: Int) {
        val handle = MemoryStack.stackPush().use {
            val pQueue = it.mallocPointer(1)
            vkGetDeviceQueue(logicalDevice.device, queueIndex, 0, pQueue)
            pQueue[0]
        }

        queue = VkQueue(handle, logicalDevice.device)
    }
}