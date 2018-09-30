package rain.vulkan

import org.lwjgl.vulkan.VK10.vkResetCommandPool
import org.lwjgl.vulkan.*


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

    fun create(window: Long) {
        instance = Instance();
        instance.create();

        surface = Surface()
        surface.create(window, instance)

        physicalDevice = PhysicalDevice()
        physicalDevice.create(instance, surface)
        surface.findColorFormatAndSpace(physicalDevice)

        queueFamilyIndices = findQueueFamilies(physicalDevice.device, surface)

        logicalDevice = LogicalDevice()
        logicalDevice.create(physicalDevice, queueFamilyIndices)

        deviceQueue = Queue()
        deviceQueue.create(logicalDevice, queueFamilyIndices.graphicsFamily)
    }
}
