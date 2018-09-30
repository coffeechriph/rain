package rain.vulkan

import org.joml.Vector3f
import rain.api.Texture2d

internal data class VulkanMaterial(val vertexShader: ShaderModule, val fragmentShader: ShaderModule, val texture2d: Texture2d, val color: Vector3f)
