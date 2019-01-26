package rain

import org.lwjgl.glfw.GLFW
import rain.api.Api
import rain.api.Input
import rain.api.Timer
import rain.api.Window
import rain.api.entity.emitterManagerInit
import rain.api.entity.emitterManagerSimulate
import rain.api.entity.moveManagerSimulate
import rain.api.gfx.ResourceFactory
import rain.api.gfx.renderManagerInit
import rain.api.gui.Gui
import rain.api.scene.Scene
import rain.util.ShaderCompiler
import rain.vulkan.Vk
import rain.vulkan.VulkanRenderer
import rain.vulkan.VulkanResourceFactory
import kotlin.math.min

open class Rain {
    private val window = Window()
    private val vk = Vk()
    private val timer = Timer()
    private lateinit var api: Api
    private lateinit var vulkanRenderer: VulkanRenderer
    protected lateinit var stateManager: StateManager
    private val input = Input()
    private lateinit var resourceFactory: ResourceFactory
    private lateinit var gui: Gui
    private lateinit var scene: Scene

    var showMouse = true
        set(value) {
            val flag = if (value) {
                GLFW.GLFW_CURSOR_NORMAL
            } else {
                GLFW.GLFW_CURSOR_HIDDEN
            }
            GLFW.glfwSetInputMode(window.windowPointer, GLFW.GLFW_CURSOR, flag)
            field = value
        }

    fun create(width: Int, height: Int, title: String, api: Api) {
        ShaderCompiler().findAndCompile()
        this.api = api
        window.create(width, height, title, input)

        when(api) {
            Api.VULKAN -> createVulkanApi()
            Api.OPENGL -> throw NotImplementedError("OpenGL API not implemented yet!")
        }

        scene = Scene(resourceFactory)
        gui = Gui(resourceFactory, vulkanRenderer)
        stateManager = StateManager(resourceFactory, scene, gui, input)
    }

    private fun createVulkanApi() {
        vk.create(window.windowPointer)
        vulkanRenderer = VulkanRenderer(vk, window)
        vulkanRenderer.create()
        resourceFactory = VulkanResourceFactory(vk)
    }

    open fun init() {
    }

    fun run() {
        startLog()
        emitterManagerInit(resourceFactory)
        gui.init()
        scene.init(resourceFactory)
        init()

        var currentTime = System.nanoTime() / 1000_000_000.0f
        val maxUpdates = 10

        while (window.pollEvents()) {
            timer.update()
            if (!stateManager.switchState) {
                // Ensure we update the physics enough times even under low fps
                val newTime = System.nanoTime() / 1000_000_000.0f
                var frameTime = newTime - currentTime
                var numUpdates = 0
                currentTime = newTime
                while (frameTime > 0.0f && numUpdates < maxUpdates) {
                    val deltaTime = min(frameTime, 1.0f / 60.0f)
                    updateLoop(deltaTime)
                    frameTime -= deltaTime
                    numUpdates++
                }

                scene.render(vulkanRenderer, resourceFactory)
                gui.render()
                vulkanRenderer.render()
            }
            else {
                stateManager.switchState = false
                scene.clear()
                gui.clear()
                vulkanRenderer.clearPipelines()
                resourceFactory.clear()

                emitterManagerInit(resourceFactory)
                gui.init()
                scene.init(resourceFactory)

                // TODO: Having initNextState returning true = game exit is kinda ugly
                if(stateManager.initNextState()) {
                    break
                }
            }

            when (api) {
                Api.VULKAN -> (resourceFactory as VulkanResourceFactory).manageResources()
                Api.OPENGL -> throw NotImplementedError("OpenGL API not implemented yet!")
            }
        }

        vulkanRenderer.destroy()
        window.destroy()
        endLog()
    }

    private fun updateLoop(deltaTime: Float) {
        window.title = "FPS: " + timer.framesPerSecond
        vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || window.windowDirty
        window.windowDirty = false

        gui.update(input)
        moveManagerSimulate()
        emitterManagerSimulate()
        scene.update(input, deltaTime)
        stateManager.update(deltaTime)
        input.updateKeyState()
    }
}
