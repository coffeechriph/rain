package rain.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR

data class SwapChainSupportDetails(var capabilities: VkSurfaceCapabilitiesKHR, val formats: VkSurfaceFormatKHR.Buffer, val presentModes: Array<Int>) {
}

// TODO: Class mismatch - use PhysicalDevice
internal fun querySwapChainSupport(device: VkPhysicalDevice, surface: Surface): SwapChainSupportDetails {
    val capabilities = VkSurfaceCapabilitiesKHR.calloc()
    KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface.surface, capabilities)

    val details = MemoryStack.stackPush().use {
        val formatCount = it.mallocInt(1)
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.surface, formatCount, null)

        val pFormats = VkSurfaceFormatKHR.calloc(formatCount[0])
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.surface, formatCount, pFormats)

        val presentModeCount = it.mallocInt(1)
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.surface, presentModeCount, null)

        val modes = it.mallocInt(presentModeCount[0])
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.surface, presentModeCount, modes)

        val modeArray = Array(presentModeCount[0]) { i -> modes[i] }

        SwapChainSupportDetails(capabilities, pFormats, modeArray)
    }
    return details
}