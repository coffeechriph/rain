package rain.vulkan

import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFormatProperties

internal fun findDepthFormat(physicalDevice: PhysicalDevice): Int {
    return findSupportedFormat(physicalDevice,
            intArrayOf(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
            VK_IMAGE_TILING_OPTIMAL,
            VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT)
}

internal fun hasStencilComponent(format: Int): Boolean {
    return format == VK_FORMAT_D32_SFLOAT_S8_UINT || format == VK_FORMAT_D24_UNORM_S8_UINT
}

internal fun findSupportedFormat(physicalDevice: PhysicalDevice, formats: IntArray, tiling: Int, features: Int): Int {
    return VK_FORMAT_D32_SFLOAT
    /*for (format in formats) {
        val props = VkFormatProperties.calloc()
        vkGetPhysicalDeviceFormatProperties(physicalDevice.device, format, props)

        println("$tiling, ${props.optimalTilingFeatures()}, $features")
        if (tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() or features) == features) {
            return format
        }
        else if (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() or features) == features) {
            return format;
        }
    }

    throw AssertionError("Failed to find supported format!")*/
}