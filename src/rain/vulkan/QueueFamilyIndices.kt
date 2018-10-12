package rain.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkQueueFamilyProperties
import rain.assertion

internal data class QueueFamilyIndices(var graphicsFamily: Int = -1, var presentFamily: Int = -1) {
    val isComplete get()= graphicsFamily >= 0 && presentFamily >= 0
}

// TODO: Class mismatch - Should take a physicalDevice
internal fun findGraphicsAndPresentFamily(device: VkPhysicalDevice, surface: Surface): QueueFamilyIndices {
    val indices = QueueFamilyIndices()
    val count = MemoryUtil.memAllocInt(1)
    VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, null)
    val families = VkQueueFamilyProperties.calloc(count[0])
    VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, families)

    var i = 0
    families.forEach {  queueFamily ->
        if(queueFamily.queueCount() > 0 && queueFamily.queueFlags() and VK10.VK_QUEUE_GRAPHICS_BIT != 0) {
            indices.graphicsFamily = i
        }

        val presentSupport = MemoryStack.stackPush().use {
            val pSupport = it.mallocInt(1)
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface.surface, pSupport)
            pSupport[0] == VK10.VK_TRUE
        }

        if(queueFamily.queueCount() > 0 && presentSupport) {
            indices.presentFamily = i
        }

        if(indices.isComplete)
            return@forEach
        i++
    }

    return indices
}

internal fun findTransferFamily(device: VkPhysicalDevice): Int {
    var index = -1
    val count = MemoryUtil.memAllocInt(1)
    VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, null)
    val families = VkQueueFamilyProperties.calloc(count[0])
    VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, families)

    var i = 0
    families.forEach {  queueFamily ->
        if(queueFamily.queueCount() > 0 && queueFamily.queueFlags() and VK10.VK_QUEUE_TRANSFER_BIT != 0) {
            index = i
            return@forEach
        }
        i++
    }

    if (index == -1) {
        assertion("Unable to find a queue family that only support transfers!")
    }

    return index
}
