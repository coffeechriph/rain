package rain.vulkan

import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import rain.assertion
import rain.log
import java.nio.ByteBuffer


internal class Instance {
    lateinit var instance: VkInstance
        private set

    private val validationLayers = arrayOf("VK_LAYER_LUNARG_core_validation")
    private var enableValidationLayers = false

    fun create() {
        val appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("Hello Rain!"))
                .pEngineName(memUTF8(""))
                .apiVersion(VK_MAKE_VERSION(1, 0, 2))

        val requiredExtensions = getRequiredExtensions()
        val ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining())
        ppEnabledExtensionNames.put(requiredExtensions)
        ppEnabledExtensionNames.flip()

        val pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(NULL)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames)

        val pInstance = memAllocPointer(1)
        val err = vkCreateInstance(pCreateInfo, null, pInstance)
        val instance = pInstance.get(0)
        memFree(pInstance)
        if (err != VK_SUCCESS) {
            assertion("Failed to create VkInstance: " + VulkanResult(err))
        }

        this.instance = VkInstance(instance, pCreateInfo)
        pCreateInfo.free()
        memFree(ppEnabledExtensionNames)
        memFree(appInfo.pApplicationName())
        memFree(appInfo.pEngineName())
        appInfo.free()
    }

    private fun checkExtensions() {
        MemoryStack.stackPush().use {
            val countBuffer = it.mallocInt(1)
            vkEnumerateInstanceExtensionProperties(null as? ByteBuffer?, countBuffer, null)
            val extensions = VkExtensionProperties.calloc(countBuffer[0])
            vkEnumerateInstanceExtensionProperties(null as? ByteBuffer?, countBuffer, extensions)

            extensions.forEach { extension ->
                log("Found extension ${extension.extensionNameString()}")
            }
        }
    }

    private fun getRequiredExtensions(): PointerBuffer {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()!!
        if(enableValidationLayers) {
            val extensions = BufferUtils.createPointerBuffer(glfwExtensions.capacity() + 1)
            extensions.put(glfwExtensions)
            extensions.put(memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
            extensions.flip()
            return extensions
        }
        return glfwExtensions
    }
}
