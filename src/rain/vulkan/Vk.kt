package rain.vulkan

import org.lwjgl.vulkan.VK10.vkResetCommandPool
import org.lwjgl.vulkan.*


internal class Vk {
    lateinit var instance: Instance
    lateinit var logicalDevice: LogicalDevice
    lateinit var physicalDevice: PhysicalDevice
    lateinit var surface: Surface
    lateinit var queueFamilyIndices: QueueFamilyIndices
    lateinit var deviceQueue: Queue
    lateinit var swapchain: Swapchain
    lateinit var renderpass: Renderpass
    lateinit var commandPool: CommandPool
    lateinit var setupCommandBuffer: CommandPool.CommandBuffer
    lateinit var postPresentCommandBuffer: CommandPool.CommandBuffer

    lateinit var vertexShader: ShaderModule
    lateinit var fragmentShader: ShaderModule

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
        postPresentCommandBuffer = commandPool.createCommandBuffer(logicalDevice.device, 1)[0]

        renderpass = Renderpass()
        renderpass.create(logicalDevice, surface.format)

        vertexShader = ShaderModule()
        vertexShader.loadShader(logicalDevice, "data/shaders/basic.vert.spv", VK10.VK_SHADER_STAGE_VERTEX_BIT)

        fragmentShader = ShaderModule()
        fragmentShader.loadShader(logicalDevice, "data/shaders/basic.frag.spv", VK10.VK_SHADER_STAGE_FRAGMENT_BIT)

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
