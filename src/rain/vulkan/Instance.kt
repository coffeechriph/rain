package rain.vulkan

import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.assertion
import rain.log
import java.nio.ByteBuffer
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.EXTDebugReport.*
import java.nio.LongBuffer
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT
import org.lwjgl.vulkan.VkDebugReportCallbackEXT
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_DEBUG_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT
import org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_HOST_MEMORY
import org.lwjgl.vulkan.VK10.VK_SUCCESS









internal class Instance {
    lateinit var instance: VkInstance
        private set

    private var enableValidationLayers = true

    private val dbgFunc = VkDebugReportCallbackEXT.create { flags, objectType, `object`, location, messageCode, pLayerPrefix, pMessage, pUserData ->
        val type: String
        if (flags and VK_DEBUG_REPORT_INFORMATION_BIT_EXT != 0) {
            type = "INFORMATION"
        } else if (flags and VK_DEBUG_REPORT_WARNING_BIT_EXT != 0) {
            type = "WARNING"
        } else if (flags and VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT != 0) {
            type = "PERFORMANCE WARNING"
        } else if (flags and VK_DEBUG_REPORT_ERROR_BIT_EXT != 0) {
            type = "ERROR"
        } else if (flags and VK_DEBUG_REPORT_DEBUG_BIT_EXT != 0) {
            type = "DEBUG"
        } else {
            type = "UNKNOWN"
        }
        System.err.format(
                "%s: [%s] Code %d : %s\n",
                type, memASCII(pLayerPrefix), messageCode, VkDebugReportCallbackEXT.getString(pMessage))
        /*
          * false indicates that layer should not bail-out of an
          * API call that had validation failures. This may mean that the
          * app dies inside the driver due to invalid parameter(s).
          * That's what would happen without validation layers, so we'll
          * keep that behavior here.
          */
        VK_FALSE
    }

    fun create() {
        val appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("Hello Rain!"))
                .pEngineName(memUTF8(""))
                .apiVersion(VK_MAKE_VERSION(1, 0, 2))

        val requiredExtensions = getRequiredExtensions()
        val ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining()+1)
        ppEnabledExtensionNames.put(requiredExtensions)
        ppEnabledExtensionNames.flip()

        val pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(NULL)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames)

        val validationLayers = getValidationLayers()
        if (validationLayers != null) {
            pCreateInfo.ppEnabledLayerNames(validationLayers)
        }

        checkExtensions()
        checkValidationLayers()

        val dbgCreateInfo: VkDebugReportCallbackCreateInfoEXT = VkDebugReportCallbackCreateInfoEXT.malloc()
                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(NULL)
                .flags(VK_DEBUG_REPORT_ERROR_BIT_EXT or VK_DEBUG_REPORT_WARNING_BIT_EXT)
                .pfnCallback(dbgFunc)
                .pUserData(NULL)
        pCreateInfo.pNext(dbgCreateInfo.address())

        val pInstance = memAllocPointer(1)
        var err = vkCreateInstance(pCreateInfo, null, pInstance)
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

        val lp = memAllocLong(1)
       /* err = vkCreateDebugReportCallbackEXT(this.instance, dbgCreateInfo, null, lp)

        when (err) {
            VK_SUCCESS ->
                msg_callback = lp.get(0)
            VK_ERROR_OUT_OF_HOST_MEMORY ->
                throw IllegalStateException("CreateDebugReportCallback: out of host memory")
            else ->
                throw IllegalStateException("CreateDebugReportCallback: unknown failure")
        }*/
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

    private fun checkValidationLayers() {
        MemoryStack.stackPush().use {
            val countBuffer = it.mallocInt(1)
            vkEnumerateInstanceLayerProperties(countBuffer, null)
            val extensions = VkLayerProperties.calloc(countBuffer[0])
            vkEnumerateInstanceLayerProperties(countBuffer, extensions)

            extensions.forEach { extension ->
                log("Found extension ${extension.layerNameString()}")
            }
        }
    }

    private fun getValidationLayers(): PointerBuffer? {
        if (enableValidationLayers) {
            val ip = memAllocInt(1)
            VK10.vkEnumerateInstanceLayerProperties(ip, null)
            val lp = VkLayerProperties.malloc(ip.get(0))
            VK10.vkEnumerateInstanceLayerProperties(ip, lp)

            val pb = memAllocPointer(4)
            var index = 0
            for (i in 0 until ip[0]) {
                log("Validation layer: ${lp[i].layerNameString()} \"${lp[i].descriptionString()}\"")
                if (lp[i].layerNameString() == "VK_LAYER_LUNARG_core_validation" ||
                    lp[i].layerNameString() == "VK_LAYER_LUNARG_parameter_validation" ||
                    lp[i].layerNameString() == "VK_LAYER_LUNARG_standard_validation" ||
                    lp[i].layerNameString() == "VK_LAYER_RENDERDOC_Capture") {
                    pb.put(memUTF8(lp[i].layerNameString()))
                    index += 1
                }
            }
            pb.flip()
            return pb
        }

        return null
    }

    private fun getRequiredExtensions(): PointerBuffer {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()!!
        /* NOT SUPPORTED ON MOLTENVK */
        if(enableValidationLayers) {
            val extensions = BufferUtils.createPointerBuffer(glfwExtensions.capacity() + 2)
            extensions.put(glfwExtensions)
            //extensions.put(memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
            extensions.put(memUTF8("VK_EXT_debug_utils"))
            extensions.flip()
            return extensions
        }
        return glfwExtensions
    }
}
