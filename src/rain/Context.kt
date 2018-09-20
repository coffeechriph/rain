package rain;
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported

internal class Context {
    var windowPointer: Long = -1
    var title: String = ""
        get() = this.toString()
        set(value) {
            glfwSetWindowTitle(windowPointer, value);
            field = value;
        }
    var windowDirty = false

    fun create(width: Int, height: Int, title: String) {
        if(!glfwInit()) {
            error("Could not init GLFW")
        }
        if (!glfwVulkanSupported()) {
            error("Vulkan is not supported by GLFW")
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        windowPointer = glfwCreateWindow(width, height, title, 0, 0)
        this.title = title;
        createCallbacks();
    }

    private fun createCallbacks() {
        // TODO: We can log this to a file
        GLFWErrorCallback.createPrint(System.err).set()

        // TODO: Forward this to a Input class
        glfwSetKeyCallback(windowPointer) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true)
            }
        }

        glfwSetWindowSizeCallback(windowPointer) {window, width, height ->
            windowDirty = true
        }
    }

    fun pollEvents(): Boolean {
        glfwPollEvents()
        return !glfwWindowShouldClose(windowPointer);
    }

    fun destroy() {
        glfwFreeCallbacks(windowPointer);
        glfwDestroyWindow(windowPointer);

        glfwTerminate();
        glfwSetErrorCallback(null)?.free();
    }
}

