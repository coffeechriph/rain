package rain.vulkan

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import rain.assertion
import rain.util.readFileAsByteBuffer

internal class ShaderModule(val id: Long) {
    var isValid = true
        private set

    lateinit var createInfo: VkPipelineShaderStageCreateInfo
        private set

    var moduleId: Long = 0
        private set

    fun invalidate() {
        isValid = false
    }

    private fun load(logicalDevice: LogicalDevice, path: String): Long {
        val shaderCode = readFileAsByteBuffer(path)
        val err: Int
        val moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pNext(0)
                .pCode(shaderCode)
                .flags(0)
        val pShaderModule = memAllocLong(1)
        err = vkCreateShaderModule(logicalDevice.device, moduleCreateInfo, null, pShaderModule)
        val shaderModule = pShaderModule.get(0)
        memFree(pShaderModule)
        if (err != VK_SUCCESS) {
            assertion("Failed to create shader module: " + VulkanResult(err))
        }

        moduleId = shaderModule
        isValid = true
        return shaderModule
    }

    fun loadShader(logicalDevice: LogicalDevice, path: String, stage: Int) {
        createInfo = VkPipelineShaderStageCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(stage)
                .module(load(logicalDevice, path))
                .pName(memUTF8("main"))
    }
}
