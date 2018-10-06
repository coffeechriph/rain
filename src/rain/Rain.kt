package rain

import org.joml.Vector3f
import rain.api.*
import rain.vulkan.*
import java.util.concurrent.atomic.AtomicBoolean

open class Rain {
    private val context = Window()
    private val vk = Vk()
    lateinit var resourceFactory: ResourceFactory
        private set
    private lateinit var vulkanRenderer: VulkanRenderer
        private set
    val scene = Scene()
    private val timer = Timer()

    fun create(width: Int, height: Int, title: String, api: Api) {
        context.create(width, height, title)

        when(api) {
            Api.VULKAN -> createVulkanApi()
            Api.OPENGL -> throw NotImplementedError("OpenGL API not implemented yet!")
        }
    }

    private fun createVulkanApi() {
        vk.create(context.windowPointer)
        resourceFactory = VulkanResourceFactory(vk)
        vulkanRenderer = VulkanRenderer(vk, resourceFactory as VulkanResourceFactory)
        vulkanRenderer.create()
    }

    open fun init() {}
    open fun update() {}

    fun run() {
        init()
        while (context.pollEvents()) {
            timer.update()
            context.title = "FPS: " + timer.framesPerSecond
            vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || context.windowDirty
            context.windowDirty = false

            if(vulkanRenderer.recreateSwapchain(vk.surface)) {
                vulkanRenderer.recreateRenderCommandBuffers()
            }

            scene.update(vulkanRenderer)
            update()
            vulkanRenderer.render()
        }

        vulkanRenderer.destroy()
        context.destroy();
    }
}
