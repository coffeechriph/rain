package rain.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer

internal class PhysicalDevice {
    lateinit var device: VkPhysicalDevice
    lateinit var memoryProperties: VkPhysicalDeviceMemoryProperties
    var heapSize: Long = 0
    val deviceExtensions = arrayOf(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)

    fun create(instance: Instance, surface: Surface) {
        val deviceCount = memAllocInt(1)
        vkEnumeratePhysicalDevices(instance.instance, deviceCount, null)
        val devicePointers = memAllocPointer(deviceCount[0])
        vkEnumeratePhysicalDevices(instance.instance, deviceCount, devicePointers)

        var graphicsCard: VkPhysicalDevice? = null
        for(i in 0 until deviceCount[0]) {
            val handle = devicePointers[i]
            val device = VkPhysicalDevice(handle, instance.instance)

            if(isDeviceSuitable(device, surface)) {
                graphicsCard = device
                break
            }
        }

        device = graphicsCard ?: error("No suitable physical device found!")

        val properties = VkPhysicalDeviceProperties.malloc()
        memoryProperties = VkPhysicalDeviceMemoryProperties.malloc()

        vkGetPhysicalDeviceProperties(device, properties)
        vkGetPhysicalDeviceMemoryProperties(device, memoryProperties)

        println("Selected Physical device:")
        println("\tDevice Name [${properties.deviceNameString()}]")
        for(i in 0 until memoryProperties.memoryHeapCount()) {
            // TODO: This will not be correct if there are multiple physical devices
            heapSize = memoryProperties.memoryHeaps(i).size()
            println("\tMemory Heap[size: ${memoryProperties.memoryHeaps(i).size() / 1024 / 1024}MB, flags:${memoryProperties.memoryHeaps(i).flags()}]");
        }

        properties.free()
    }

    private fun isDeviceSuitable(device: VkPhysicalDevice, surface: Surface): Boolean {
        val indices = findQueueFamilies(device, surface)
        if(!indices.isComplete) {
            return false
        }

        if (!checkDeviceExtensionSupport(device)) {
            return false
        }

        val swapChainSupport = querySwapChainSupport(device, surface)
        if(!swapChainSupport.formats.hasRemaining() || swapChainSupport.presentModes.isEmpty())
            return false

        return true
    }

    private fun checkDeviceExtensionSupport(device: VkPhysicalDevice): Boolean {
        return MemoryStack.stackPush().use {
            val count = it.mallocInt(1)
            vkEnumerateDeviceExtensionProperties(device, null as? ByteBuffer, count, null)

            val availableExtensions = VkExtensionProperties.callocStack(count[0])
            vkEnumerateDeviceExtensionProperties(device, null as? ByteBuffer, count, availableExtensions)

            val requiredExtensions = mutableListOf(*deviceExtensions)
            availableExtensions.forEach {
                requiredExtensions -= it.extensionNameString()
            }

            if(requiredExtensions.isNotEmpty()) {
                println("Missing extensions:")
                for(required in requiredExtensions) {
                    println("\t- $required")
                }
            }
            requiredExtensions.isEmpty()
        }
    }
}