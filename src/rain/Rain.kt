package rain

import rain.api.*
import rain.api.gfx.ResourceFactory
import rain.api.gui.Button
import rain.api.gui.Font
import rain.api.gui.Gui
import rain.api.gui.ToggleButton
import rain.api.scene.Scene
import rain.vulkan.*

open class Rain {
    private val context = Window()
    private val vk = Vk()
    private val timer = Timer()
    private lateinit var vulkanRenderer: VulkanRenderer
    private lateinit var stateManager: StateManager
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
            if (!stateManager.switchState) {
                timer.update()
                context.title = "FPS: " + timer.framesPerSecond
                vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || context.windowDirty
                context.windowDirty = false

                gui.update(input)
                scene.update(vulkanRenderer, input, timer.deltaTime)
                update()
                stateManager.update()
                gui.render()
                vulkanRenderer.render()

                input.updateKeyState()
            }
            else {
                scene.clear()
                gui.clear()
                resourceFactory.clear()

                gui.init()
                vulkanRenderer.cleanSoftResources()
            }
        }

        vulkanRenderer.destroy()
        context.destroy();
        endLog()
    }
}
