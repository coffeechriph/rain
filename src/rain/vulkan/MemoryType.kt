package rain.vulkan

import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties
import java.nio.IntBuffer

internal fun getMemoryType(deviceMemoryProperties: VkPhysicalDeviceMemoryProperties, typeBits: Int, properties: Int, typeIndex: IntBuffer): Boolean {
    var bits = typeBits
    for (i in 0..31) {
        if (bits and 1 == 1) {
            if (deviceMemoryProperties.memoryTypes(i).propertyFlags() and properties == properties) {
                typeIndex.put(0, i)
                return true
            }
        }
        bits = bits shr 1
    }
    return false
}