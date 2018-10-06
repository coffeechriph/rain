package rain.vulkan

import org.joml.Vector3f
import org.lwjgl.vulkan.VK10
import rain.api.Material
import rain.api.Texture2d

internal class VulkanMaterial: Material {
    internal val vertexShader: ShaderModule
    internal val fragmentShader: ShaderModule
    internal val texture2d: VulkanTexture2d
    internal val color: Vector3f
    internal val descriptorPool: DescriptorPool

    constructor(logicalDevice: LogicalDevice, vertexShader: ShaderModule, fragmentShader: ShaderModule, texture2d: VulkanTexture2d, color: Vector3f) {
        this.vertexShader = vertexShader
        this.fragmentShader = fragmentShader
        this.texture2d = texture2d
        this.color = color

        descriptorPool = DescriptorPool()
                .withTexture(texture2d, VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                .build(logicalDevice)
    }
}
