package rain

import org.joml.Vector3f
import rain.api.Renderer
import rain.api.ResourceFactory
import rain.api.Texture2d
import rain.api.TextureFilter
import rain.vulkan.*

class Rain {
    private val context = Window()
    private val vk = Vk()
    private lateinit var resourceFactory: VulkanResourceFactory
        private set
    private lateinit var vulkanRenderer: VulkanRenderer
        private set
    private val timer = Timer()

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
        resourceFactory = VulkanResourceFactory(vk)
        vulkanRenderer = VulkanRenderer(vk, resourceFactory)
        vulkanRenderer.create()

        resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", Texture2d(0, 0, 0, TextureFilter.LINEAR), Vector3f())
        resourceFactory.createTexture2d("./data/textures/town.png", TextureFilter.NEAREST)
    }

    fun run() {
        while (context.pollEvents()) {
            timer.update()
            context.title = "FPS: " + timer.framesPerSecond
            vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || context.windowDirty
            context.windowDirty = false

            if(vulkanRenderer.recreateSwapchain(vk.surface)) {
                vulkanRenderer.recreateRenderCommandBuffers()
            }

            vulkanRenderer.render()
        }

        context.destroy();
    }
}
