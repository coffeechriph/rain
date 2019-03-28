package rain

import org.lwjgl.glfw.GLFW
import rain.api.Api
import rain.api.Input
import rain.api.Timer
import rain.api.WindowContext
import rain.api.gfx.ResourceFactory
import rain.api.gui.v2.*
import rain.api.manager.*
import rain.api.scene.Scene
import rain.util.ShaderCompiler
import rain.vulkan.Vk
import rain.vulkan.VulkanRenderer
import rain.vulkan.VulkanResourceFactory

open class Rain {
    protected val window = WindowContext()
    private val vk = Vk()
    private val timer = Timer()
    private lateinit var api: Api
    private lateinit var vulkanRenderer: VulkanRenderer
    protected lateinit var stateManager: StateManager
    private val input = Input()
    private lateinit var resourceFactory: ResourceFactory
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

        scene = Scene(resourceFactory, window)
        stateManager = StateManager(resourceFactory, scene)
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
        guiManagerInit(resourceFactory)
        scene.init(resourceFactory)
        init()

        var oldTime = System.nanoTime() / 1000_000_000.0
        var accumulator = 0.0

        while (window.pollEvents()) {
            timer.update()

            if (!stateManager.switchState) {
                // Ensure we update the physics enough times even under low fps
                val currentTime = System.nanoTime() / 1000_000_000.0
                val deltaTime = currentTime - oldTime
                oldTime = currentTime
                accumulator += deltaTime
                accumulator = Math.min(8.0/60.0, accumulator)
                while (accumulator > 1.0f / 61.0) {
                    updateLoop(deltaTime)
                    accumulator -= 1.0f/59.0
                    if (accumulator < 0) {
                        accumulator = 0.0;
                    }
                }

                scene.render(vulkanRenderer)
                guiManagerHandleGfx()
                vulkanRenderer.render()
            }
            else {
                stateManager.switchState = false
                scene.clear()
                guiManagerClear()
                emitterManagerClear()
                renderManagerClear()
                vulkanRenderer.clearPipelines()
                resourceFactory.clear()

                emitterManagerInit(resourceFactory)
                guiManagerInit(resourceFactory)
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

    private fun updateLoop(deltaTime: Double) {
        window.title = "FPS: " + timer.framesPerSecond
        vulkanRenderer.swapchainIsDirty = vulkanRenderer.swapchainIsDirty || window.windowDirty
        window.windowDirty = false

        guiManagerHandleInput(input)

        // Allow the UI to steal input events
        var localInput = input
        if (guiManagerShouldStealInput()) {
            localInput = Input()
        }

        moveManagerSimulate()
        emitterManagerSimulate()
        animatorManagerSimulate()
        scene.update(localInput)
        stateManager.update(localInput)
        input.updateKeyState()
    }
}
