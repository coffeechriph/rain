package rain.vulkan

import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import rain.assertion

internal fun createBuffer(logicalDevice: LogicalDevice, size: Long, usage: Int, properties: Int, memoryProperties: VkPhysicalDeviceMemoryProperties): VulkanVertexBuffer.Buffer {
    val bufInfo = VkBufferCreateInfo.calloc()
            .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            .pNext(0)
            .size(size)
            .usage(usage)
            .flags(0)

    val pBuffer = MemoryUtil.memAllocLong(1)
    var err = VK10.vkCreateBuffer(logicalDevice.device, bufInfo, null, pBuffer)

    val buffer = pBuffer.get(0)
    MemoryUtil.memFree(pBuffer)
    bufInfo.free()
    if (err != VK10.VK_SUCCESS) {
        assertion("Failed to create vertex buffer: " + VulkanResult(err))
    }

    val memReqs = VkMemoryRequirements.calloc()
    VK10.vkGetBufferMemoryRequirements(logicalDevice.device, buffer, memReqs)

    val memAlloc = VkMemoryAllocateInfo.calloc()
            .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            .pNext(0)
            .allocationSize(memReqs.size())
            .memoryTypeIndex(0)

    val memoryTypeIndex = MemoryUtil.memAllocInt(1)
    getMemoryType(memoryProperties, memReqs.memoryTypeBits(), properties, memoryTypeIndex)
    memAlloc.memoryTypeIndex(memoryTypeIndex.get(0))

    MemoryUtil.memFree(memoryTypeIndex)
    memReqs.free()

    val bufferMemory = MemoryUtil.memAllocLong(1)
    val bufferSize = memAlloc.allocationSize()
    err = VK10.vkAllocateMemory(logicalDevice.device, memAlloc, null, bufferMemory)

    if (err != VK10.VK_SUCCESS) {
        assertion("Failed to allocate vertex memory: " + VulkanResult(err))
    }

    err = VK10.vkBindBufferMemory(logicalDevice.device, buffer, bufferMemory.get(0), 0)
    if (err != VK10.VK_SUCCESS) {
        assertion("Failed to bind memory to vertex buffer: " + VulkanResult(err))
    }

    return VulkanVertexBuffer.Buffer(buffer, bufferMemory.get(), bufferSize)
}
