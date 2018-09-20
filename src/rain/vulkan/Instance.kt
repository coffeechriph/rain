package rain.vulkan

import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VK10.VK_MAKE_VERSION
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO
import org.lwjgl.vulkan.VkApplicationInfo



internal class Instance {
    lateinit var instance: VkInstance
    val validationLayers = arrayOf("VK_LAYER_LUNARG_core_validation")
    var enableValidationLayers = false

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
            throw AssertionError("Failed to create VkInstance: " + VulkanResult(err))
        }

        this.instance = VkInstance(instance, pCreateInfo)
        pCreateInfo.free()
        memFree(ppEnabledExtensionNames)
        memFree(appInfo.pApplicationName())
        memFree(appInfo.pEngineName())
        appInfo.free()
    }

    private fun checkValidationLayers() {
        MemoryStack.stackPush().use {
            val countBuffer = it.mallocInt(1)
            vkEnumerateInstanceLayerProperties(countBuffer, null)
            val layers = VkLayerProperties.calloc(countBuffer[0])
            vkEnumerateInstanceLayerProperties(countBuffer, layers)

            for(layerName in validationLayers) {
                var found = false

                layers.forEach { layer ->
                    if(layer.layerNameString() == layerName) {
                        found = true
                        return@forEach
                    }
                }

                if(!found) {
                    error("Missing validation layer '$layerName'")
                }
            }
        }

        println("Found all validation layers")
    }

    private fun checkExtensions() {
        MemoryStack.stackPush().use {
            val countBuffer = it.mallocInt(1)
            vkEnumerateInstanceExtensionProperties(null as? ByteBuffer, countBuffer, null)
            val extensions = VkExtensionProperties.calloc(countBuffer[0])
            vkEnumerateInstanceExtensionProperties(null as? ByteBuffer, countBuffer, extensions)

            extensions.forEach { extension ->
                println("Found extension ${extension.extensionNameString()}")
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