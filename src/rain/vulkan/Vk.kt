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
    lateinit var swapchain: Swapchain
        private set
    lateinit var renderpass: Renderpass
        private set

    internal lateinit var commandPool: CommandPool
    internal lateinit var setupCommandBuffer: CommandPool.CommandBuffer

    var swapchainIsDirty = true

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

        commandPool = CommandPool()
        commandPool.create(logicalDevice, queueFamilyIndices.graphicsFamily)

        setupCommandBuffer = commandPool.createCommandBuffer(logicalDevice.device, 1)[0]

        renderpass = Renderpass()
        renderpass.create(logicalDevice, surface.format)

        swapchain = Swapchain()
    }

    fun recreateSwapchain(): Boolean {
        if (swapchainIsDirty) {
            val capabilities = VkSurfaceCapabilitiesKHR.calloc()
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.device, surface.surface, capabilities)

            swapchain.create(logicalDevice, physicalDevice, surface, setupCommandBuffer, deviceQueue)
            swapchain.createFramebuffers(logicalDevice, renderpass, capabilities.currentExtent())
            swapchainIsDirty = false
            return true
        }

        return false
    }
}
