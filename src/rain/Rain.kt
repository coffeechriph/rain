package rain

import rain.api.*
import rain.api.gfx.ResourceFactory
import rain.api.gui.Gui
import rain.api.scene.Scene
import rain.vulkan.Vk
import rain.vulkan.VulkanRenderer
import rain.vulkan.VulkanResourceFactory

open class Rain {
    private val context = Window()
    private val vk = Vk()
    private val timer = Timer()
    private lateinit var vulkanRenderer: VulkanRenderer
    internal lateinit var stateManager: StateManager
    internal val input = Input()
    lateinit var resourceFactory: ResourceFactory
        private set
    lateinit var gui: Gui
        private set
    val scene = Scene()

    fun create(width: Int, height: Int, title: String, api: Api) {
        context.create(width, height, title, input)

        when(api) {
            Api.VULKAN -> createVulkanApi()
            Api.OPENGL -> throw NotImplementedError("OpenGL API not implemented yet!")
        }

        gui = Gui(resourceFactory, vulkanRenderer)
        gui.init()

        stateManager = StateManager(resourceFactory, scene, gui, input)
    }

    private fun createVulkanApi() {
        vk.create(context.windowPointer)
        vulkanRenderer = VulkanRenderer(vk, context)
        vulkanRenderer.create()
        resourceFactory = VulkanResourceFactory(vk, vulkanRenderer)
    }

    open fun init() {}

    fun run() {
        startLog()
        scene.init(resourceFactory)
        init()

        while (context.pollEvents()) {
            if (!stateManager.switchState) {
                timer.update()
                context.title = "FPS: " + timer.framesPerSecond
                vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || context.windowDirty
                context.windowDirty = false

                gui.update(input)
                scene.update(vulkanRenderer, input, timer.deltaTime)
                stateManager.update()

                gui.render()
                vulkanRenderer.render()

                input.updateKeyState()
            }
            else {
                vulkanRenderer.recreateResources()
                stateManager.switchState = false
                scene.clear()
                gui.clear()
                resourceFactory.clear()

                gui.init()
                scene.init(resourceFactory)

                stateManager.initNextState()
            }
        }

        vulkanRenderer.destroy()
        context.destroy();
        endLog()
    }
}
