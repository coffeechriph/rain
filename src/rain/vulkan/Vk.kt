package rain.vulkan

import org.lwjgl.system.MemoryUtil.memAllocInt
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
    var transferFamilyIndex: Int = -1
        private set
    lateinit var deviceQueue: Queue
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
    }
}
