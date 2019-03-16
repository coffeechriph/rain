package rain.api
import org.joml.Vector2i
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryUtil.memAllocInt
import rain.assertion
import rain.log

class Window internal constructor() {
    internal var windowPointer: Long = -1
    internal var windowDirty = false

    var title: String = ""
        get() = this.toString()
        set(value) {
            glfwSetWindowTitle(windowPointer, value)
            field = value
        }
    var size: Vector2i = Vector2i(0,0)
        private set
    var framebufferSize: Vector2i = Vector2i(0,0)
        private set

    internal fun create(width: Int, height: Int, title: String, input: Input) {
        if(!glfwInit()) {
            assertion("Could not init GLFW")
        }
        else {
            log("GLFW initialized properly!")
        }

        // TODO: Should only check if vulkan is supported if we decide to create a vulkan context
        if (!glfwVulkanSupported()) {
            assertion("Vulkan is not supported by GLFW")
        }
        else {
            log("Vulkan is supported!")
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        windowPointer = glfwCreateWindow(width, height, title, 0, 0)
        this.title = title
        createCallbacks(input)

        updateWindowSize()
        updateFramebufferSize()
        log("Created window[w: $width, h: $height]")
    }

    private fun updateWindowSize() {
        val width = memAllocInt(1)
        val height = memAllocInt(1)
        glfwGetWindowSize(windowPointer, width, height)
        size.x = width[0]
        size.y = height[0]
    }

    private fun updateFramebufferSize() {
        val width = memAllocInt(1)
        val height = memAllocInt(1)
        glfwGetFramebufferSize(windowPointer, width, height)
        framebufferSize.x = width[0]
        framebufferSize.y = height[0]
    }

    private fun createCallbacks(input: Input) {
        // TODO: We can log this to a file
        GLFWErrorCallback.createPrint(System.err).set()

        // TODO: Extend to properly handle mods
        glfwSetKeyCallback(windowPointer) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true)
            }
            else if (action != GLFW_REPEAT) {
                val act = if(action == GLFW_PRESS) { Input.InputState.PRESSED } else { Input.InputState.RELEASED }
                input.putKeyState(key, act)
            }
        }

        // TODO: Extend to properly handle mods
        glfwSetMouseButtonCallback(windowPointer) { _, button, action, mods ->
            if (action != GLFW_REPEAT) {
                if (action == GLFW_PRESS) {
                    input.putMouseState(button, Input.InputState.PRESSED)
                }
                else if (action == GLFW_RELEASE) {
                    input.putMouseState(button, Input.InputState.RELEASED)
                }
            }
        }

        glfwSetCharCallback(windowPointer) { _, codepoint ->
            input.triggerSingleChar(codepoint)
        }

        glfwSetCursorPosCallback(windowPointer) { _, xpos, ypos ->
            input.mousePosition.set(xpos.toInt(), ypos.toInt())
        }

        glfwSetWindowSizeCallback(windowPointer) { _, _, _ ->
            windowDirty = true
            updateWindowSize()
            updateFramebufferSize()
        }
    }

    internal fun pollEvents(): Boolean {
        glfwPollEvents()
        return !glfwWindowShouldClose(windowPointer)
    }

    internal fun destroy() {
        glfwFreeCallbacks(windowPointer)
        glfwDestroyWindow(windowPointer)

        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }
}

