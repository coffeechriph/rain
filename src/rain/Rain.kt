package rain

import rain.api.*
import rain.api.gfx.ResourceFactory
import rain.api.scene.Scene
import rain.vulkan.*

open class Rain {
    private val context = Window()
    private val vk = Vk()
    internal val input = Input()
    lateinit var resourceFactory: ResourceFactory
        private set
    private lateinit var vulkanRenderer: VulkanRenderer
    val scene = Scene()
    private val timer = Timer()

    fun create(width: Int, height: Int, title: String, api: Api) {
        context.create(width, height, title, input)

        when(api) {
            Api.VULKAN -> createVulkanApi()
            Api.OPENGL -> throw NotImplementedError("OpenGL API not implemented yet!")
        }
    }

    private fun createVulkanApi() {
        vk.create(context.windowPointer)
        resourceFactory = VulkanResourceFactory(vk)
        vulkanRenderer = VulkanRenderer(vk, context)
        vulkanRenderer.create()
    }

    open fun init() {}
    open fun update() {}

    fun run() {
        startLog()
        scene.init(resourceFactory)
        init()

        while (context.pollEvents()) {
            timer.update()
            context.title = "FPS: " + timer.framesPerSecond
            vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || context.windowDirty
            context.windowDirty = false

            scene.update(vulkanRenderer, input, timer.deltaTime)
            update()
            vulkanRenderer.render()

            input.updateKeyState()
        }

        vulkanRenderer.destroy()
        context.destroy();
        endLog()
    }
}
