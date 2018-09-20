package rain

import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR
import org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPresentInfoKHR
import rain.api.Renderer
import rain.api.ResourceFactory
import rain.api.Texture2d
import rain.api.TextureFilter
import rain.vulkan.*

class Rain {
    private val context = Context()
    private val vk = Vk()
    private lateinit var resourceFactory: VulkanResourceFactory
        private set
    private lateinit var vulkanRenderer: VulkanRenderer
        private set

    fun create(width: Int, height: Int, title: String, api: Api) {
        context.create(width, height, title)

        when(api) {
            Api.VULKAN -> createVulkanApi()
            Api.OPENGL -> throw NotImplementedError("OpenGL API not implemented yet!")
        }
    }

    fun getResourceFactory(): ResourceFactory {
        return resourceFactory
    }

    fun getRenderer(): Renderer {
        return vulkanRenderer
    }

    private fun createVulkanApi() {
        vk.create(context.windowPointer)
        resourceFactory = VulkanResourceFactory(vk.logicalDevice, vk.physicalDevice)
        vulkanRenderer = VulkanRenderer(vk.logicalDevice, vk.queueFamilyIndices, vk.swapchain, vk.deviceQueue, resourceFactory, vk.renderpass, resourceFactory.quadVertexBuffer)
        vulkanRenderer.create()

        val material = VulkanMaterial(vk.vertexShader, vk.fragmentShader, Texture2d(0, 0, 0, TextureFilter.LINEAR), Vector3f())
        vulkanRenderer.resourceFactory.materials.add(material)
    }

    fun run() {
        while (context.pollEvents()) {
            vk.swapchainIsDirty = vk.swapchainIsDirty || context.windowDirty
            context.windowDirty = false

            if(vk.recreateSwapchain()) {
                vulkanRenderer.recreateRenderCommandBuffers()
            }

            vulkanRenderer.render()
        }

        context.destroy();
    }
}
