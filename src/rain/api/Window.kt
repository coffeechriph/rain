package rain.api
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import rain.assertion
import rain.log

internal class Window {
    var windowPointer: Long = -1
    var title: String = ""
        get() = this.toString()
        set(value) {
            glfwSetWindowTitle(windowPointer, value)
            field = value
        }
    var windowDirty = false

    fun create(width: Int, height: Int, title: String, input: Input) {
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

        log("Created window[w: $width, h: $height]")
    }

    private fun createCallbacks(input: Input) {
        // TODO: We can log this to a file
        GLFWErrorCallback.createPrint(System.err).set()

        glfwSetKeyCallback(windowPointer) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true)
            }
            else if (action != GLFW_REPEAT) {
                val act = if(action == GLFW_PRESS) { Input.InputState.PRESSED } else { Input.InputState.RELEASED }
                input.putKeyState(key, act)
            }
        }

        glfwSetMouseButtonCallback(windowPointer) {window, button, action, mods ->
            if (action != GLFW_REPEAT) {
                if (action == GLFW_PRESS) {
                    input.putMouseState(button, Input.InputState.PRESSED)
                }
                else if (action == GLFW_RELEASE) {
                    input.putMouseState(button, Input.InputState.RELEASED)
                }
            }
        }

        glfwSetCharCallback(windowPointer) {window, codepoint ->
            input.triggerSingleChar(codepoint)
        }

        glfwSetCursorPosCallback(windowPointer) {window, xpos, ypos ->
            input.mousePosition.set(xpos.toInt(), ypos.toInt())
        }

        glfwSetWindowSizeCallback(windowPointer) {window, width, height ->
            windowDirty = true
        }
    }

    fun pollEvents(): Boolean {
        glfwPollEvents()
        return !glfwWindowShouldClose(windowPointer)
    }

    fun destroy() {
        glfwFreeCallbacks(windowPointer)
        glfwDestroyWindow(windowPointer)

        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
    }
}

