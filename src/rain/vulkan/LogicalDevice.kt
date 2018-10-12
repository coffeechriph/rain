package rain.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import rain.assertion

internal class LogicalDevice {
    lateinit var device: VkDevice
        private set

    fun create(physicalDevice: PhysicalDevice, indices: QueueFamilyIndices) {
        val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(2)
        val uniqueQueueFamilies = arrayOf(indices.graphicsFamily, indices.presentFamily)
        for((index, queueFamily) in uniqueQueueFamilies.withIndex()) {
            val info = queueCreateInfo[index]
            info.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
            info.queueFamilyIndex(queueFamily)
            info.pQueuePriorities(MemoryStack.stackFloats(1f))
        }

        val createInfo = VkDeviceCreateInfo.calloc()
        createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
        createInfo.pQueueCreateInfos(queueCreateInfo)

        val deviceFeatures = VkPhysicalDeviceFeatures.calloc()
        createInfo.pEnabledFeatures(deviceFeatures)
        val pExtensionNames = memAllocPointer(physicalDevice.deviceExtensions.size)
        for(extension in physicalDevice.deviceExtensions) {
            pExtensionNames.put(memUTF8(extension))
        }
        pExtensionNames.flip()
        createInfo.ppEnabledExtensionNames(pExtensionNames)

        val handle = MemoryStack.stackPush().use {
            val pDevice = it.mallocPointer(1)
            if(vkCreateDevice(physicalDevice.device, createInfo, null, pDevice) != VK_SUCCESS)
                assertion("Failed to create logical device")
            pDevice[0]
        }

        device = VkDevice(handle, physicalDevice.device, createInfo)
    }
}
