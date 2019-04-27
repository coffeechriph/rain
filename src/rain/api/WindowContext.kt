package rain.api
import org.joml.Vector2i
import org.lwjgl.bgfx.BGFX.*
import org.lwjgl.bgfx.BGFXInit
import org.lwjgl.bgfx.BGFXPlatform.bgfx_set_platform_data
import org.lwjgl.bgfx.BGFXPlatformData
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWNativeCocoa
import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.glfw.GLFWNativeX11
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.APIUtil.apiLog
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.Platform
import rain.assertion
import rain.log
import org.lwjgl.bgfx.BGFX.BGFX_PCI_ID_NONE
import org.lwjgl.bgfx.BGFX.BGFX_RENDERER_TYPE_COUNT




class WindowContext internal constructor() {
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
    internal var cameraProjectionSize = Vector2i(1,1)

    private var format: Int = 0

    private var renderer = BGFX_RENDERER_TYPE_COUNT
    private val pciId = BGFX_PCI_ID_NONE

    internal fun create(api: Api, width: Int, height: Int, title: String, input: Input) {
        if(!glfwInit()) {
            assertion("Could not init GLFW")
        }
        else {
            log("GLFW initialized properly!")
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        // TODO: Should only check if vulkan is supported if we decide to create a vulkan context
        if (api == Api.VULKAN) {
            if (!glfwVulkanSupported()) {
                assertion("Vulkan is not supported by GLFW")
            } else {
                log("Vulkan is supported!")
            }
        }
        else if (api == Api.BGFX) {
            MemoryStack.stackPush().use { stack ->
                val platformData = BGFXPlatformData.callocStack(stack)

                when (Platform.get()) {
                    Platform.LINUX -> {
                        platformData.ndt(GLFWNativeX11.glfwGetX11Display())
                        platformData.nwh(GLFWNativeX11.glfwGetX11Window(windowPointer))
                    }
                    Platform.MACOSX -> {
                        platformData.ndt(0)
                        platformData.nwh(GLFWNativeCocoa.glfwGetCocoaWindow(windowPointer))
                    }
                    Platform.WINDOWS -> {
                        platformData.ndt(0)
                        platformData.nwh(GLFWNativeWin32.glfwGetWin32Window(windowPointer))
                    }
                }

                platformData.context(0)
                platformData.backBuffer(0)
                platformData.backBufferDS(0)

                bgfx_set_platform_data(platformData)

                //BGFXDemoUtil.reportSupportedRenderers()

                /* Initialize bgfx */

                val init = BGFXInit.mallocStack(stack)
                bgfx_init_ctor(init)
                init
                        .type(renderer)
                        .vendorId(pciId)
                        .deviceId(0.toShort())
                        // .callback(if (useCallbacks) createCallbacks(stack) else null)
                        // .allocator(if (useCustomAllocator) createAllocator(stack) else null)
                        .resolution {
                            it
                                    .width(width)
                                    .height(height)
                                    .reset(BGFX_RESET_VSYNC)
                        }

                if (!bgfx_init(init)) {
                    throw RuntimeException("Error initializing bgfx renderer")
                }

                format = init.resolution().format()

                if (renderer === BGFX_RENDERER_TYPE_COUNT) {
                    renderer = bgfx_get_renderer_type()
                }

                val rendererName = bgfx_get_renderer_name(renderer)
                if ("NULL" == rendererName) {
                    throw RuntimeException("Error identifying bgfx renderer")
                }

                apiLog("bgfx: using renderer '$rendererName'")

                //BGFXDemoUtil.configure(renderer)

                bgfx_set_debug(BGFX_DEBUG_TEXT)
            }
        }

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
            val mx = (cameraProjectionSize.x.toFloat()/size.x.toFloat()) * xpos
            val my = (cameraProjectionSize.y.toFloat()/size.y.toFloat()) * ypos
            input.mousePosition.set(mx.toInt(), my.toInt())
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

