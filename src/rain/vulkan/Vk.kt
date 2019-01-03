package rain.vulkan

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.MemoryUtil.memAllocPointer
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK11
import org.lwjgl.vulkan.VkLayerProperties

internal class Vk {
    lateinit var instance: Instance
        private set
    lateinit var logicalDevice: LogicalDevice
        private set
    lateinit var physicalDevice: PhysicalDevice
        private set
    lateinit var surface: Surface
        private set
    lateinit var queueFamilyIndices: QueueFamilyIndices
        private set
    lateinit var deviceQueue: Queue
        private set

    var vmaAllocator: Long = 0
    var transferFamilyIndex: Int = -1
        private set

    fun create(window: Long) {
        instance = Instance()
        instance.create()

        surface = Surface()
        surface.create(window, instance)

        physicalDevice = PhysicalDevice()
        physicalDevice.create(instance, surface)
        surface.findColorFormatAndSpace(physicalDevice)

        queueFamilyIndices = findGraphicsAndPresentFamily(physicalDevice.device, surface)
        transferFamilyIndex = findTransferFamily(physicalDevice.device)

        logicalDevice = LogicalDevice()
        logicalDevice.create(physicalDevice, queueFamilyIndices)

        deviceQueue = Queue()
        deviceQueue.create(logicalDevice, queueFamilyIndices.graphicsFamily)

        val vmaVulkanFunctions = VmaVulkanFunctions.calloc()
                .set(instance.instance, logicalDevice.device)

        val pVmaAllocator = memAllocPointer(4)
        val vmaAllocatorCreateInfo = VmaAllocatorCreateInfo.calloc()
                .device(logicalDevice.device)
                .physicalDevice(physicalDevice.device)
                .pVulkanFunctions(vmaVulkanFunctions)

        vmaCreateAllocator(vmaAllocatorCreateInfo, pVmaAllocator)
        vmaAllocator = pVmaAllocator[0]
    }
}
