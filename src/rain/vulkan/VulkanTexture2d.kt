package rain.vulkan

import org.lwjgl.stb.STBImage.stbi_load
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.assertion
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer

internal class VulkanTexture2d(val id: Long): Texture2d {
    private var texture: Long = 0
    var textureView: Long = 0
    var textureSampler: Long = 0

    private var width = 0
    private var height = 0
    private var texCoordWidth = 1.0f
    private var texCoordHeight = 1.0f

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun getTexCoordWidth(): Float {
        return texCoordWidth
    }

    override fun getTexCoordHeight(): Float {
        return texCoordHeight
    }

    override fun setTiledTexture(tileWidth: Int, tileHeight: Int) {
        texCoordWidth = (tileWidth.toFloat() / width.toFloat())
        texCoordHeight = (tileHeight.toFloat() / height.toFloat())
    }

    fun load(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, commandPool: CommandPool, queue: VkQueue, filePath: String, filter: TextureFilter) {
        if (!File(filePath).exists()) {
            throw FileNotFoundException("File $filePath was not found!")
        }

        val width = memAllocInt(1)
        val height = memAllocInt(1)
        val channels = memAllocInt(1)

        val imageData = stbi_load(filePath, width, height, channels, 0)
        if (imageData == null) {
            throw RuntimeException("Failed to load image $filePath")
        }

        createImage(logicalDevice, memoryProperties, commandPool, queue, imageData, width.get(0), height.get(0), channels.get(0), filter)
    }

    fun createImage(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, commandPool: CommandPool, queue: VkQueue, imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter) {
        val format = findTextureFormat(channels)
        val textureFilter = when(filter) {
            TextureFilter.NEAREST -> VK_FILTER_NEAREST
            TextureFilter.LINEAR -> VK_FILTER_LINEAR
        }

        val buffer = createBuffer(logicalDevice, (width*height*channels).toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, memoryProperties)

        val imageDataBuffer = MemoryUtil.memAlloc(imageData.remaining())
        imageDataBuffer.put(imageData)
        imageDataBuffer.flip()

        val pData = MemoryUtil.memAllocPointer(1)
        var err = VK10.vkMapMemory(logicalDevice.device, buffer.bufferMemory, 0, buffer.bufferSize, 0, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK10.VK_SUCCESS) {
            assertion("Failed to map image memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(imageDataBuffer), data, imageDataBuffer.remaining().toLong())
        MemoryUtil.memFree(imageDataBuffer)
        VK10.vkUnmapMemory(logicalDevice.device, buffer.bufferMemory)

        MemoryStack.stackPush().use {
            val imageCreateInfo: VkImageCreateInfo = VkImageCreateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(format)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .flags(0)

            val extent = imageCreateInfo
                    .extent()

            extent.width(width)
                    .height(height)

            val textureImage = memAllocLong(1)
            vkCreateImage(logicalDevice.device, imageCreateInfo, null, textureImage)

            val memoryRequirements = VkMemoryRequirements.callocStack()
            vkGetImageMemoryRequirements(logicalDevice.device, textureImage.get(0), memoryRequirements)

            val typeIndex = memAllocInt(1)
            getMemoryType(memoryProperties, memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, typeIndex)

            val memoryAllocateInfo = VkMemoryAllocateInfo.callocStack()
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(typeIndex.get(0))

            val textureImageMemory = memAllocLong(1)
            err = vkAllocateMemory(logicalDevice.device, memoryAllocateInfo, null, textureImageMemory)
            if (err != VK_SUCCESS) {
                assertion("Error allocating texture memory " + VulkanResult(err))
            }

            vkBindImageMemory(logicalDevice.device, textureImage.get(0), textureImageMemory.get(0), 0)

            transitionImageLayout(logicalDevice, commandPool, queue, textureImage.get(0), format, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
            copyBufferToImage(logicalDevice, commandPool, queue, buffer.buffer, textureImage.get(0), width, height)
            transitionImageLayout(logicalDevice, commandPool, queue, textureImage.get(0), format, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

            texture = textureImage.get(0)

            val textureImageView = ImageView()
            textureImageView.create(logicalDevice, texture, format, VK_IMAGE_VIEW_TYPE_2D, VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            textureView = textureImageView.imageView
            this.width = width
            this.height = height

            // Create the texture sampler
            val samplerCreateInfo = VkSamplerCreateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(textureFilter)
                    .minFilter(textureFilter)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .anisotropyEnable(false)
                    .maxAnisotropy(0.0f)
                    .borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
                    .mipLodBias(0.0f)
                    .minLod(0.0f)
                    .maxLod(0.0f)

            val sampler = memAllocLong(1)
            err = vkCreateSampler(logicalDevice.device, samplerCreateInfo, null, sampler)
            if (err != VK_SUCCESS) {
                assertion("Unable to create texture sampler " + VulkanResult(err))
            }
            this.textureSampler = sampler.get(0)
        }
    }

    private fun findTextureFormat(channels: Int): Int {
        when(channels) {
            1 -> return VK_FORMAT_R8_UNORM
            2 -> return VK_FORMAT_R8G8_UNORM
            3 -> return VK_FORMAT_R8G8B8_UNORM
            4 -> return VK_FORMAT_R8G8B8A8_UNORM
            else -> throw AssertionError("Unsupported number of channels for texture $channels")
        }
    }

    private fun copyBufferToImage(logicalDevice: LogicalDevice, commandPool: CommandPool, queue: VkQueue, buffer: Long, image: Long, width: Int, height: Int) {
        val commandBuffer = commandPool.createCommandBuffer(logicalDevice.device, 1)[0]
        commandBuffer.begin()

        val region = VkBufferImageCopy.calloc(1)
                .bufferOffset(0)
                .bufferRowLength(0)
                .bufferImageHeight(0)
        val subresource = region.imageSubresource()
        subresource
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1)

        region.imageOffset()
                .set(0,0,0)

        region.imageExtent()
                .set(width, height, 1)

        vkCmdCopyBufferToImage(commandBuffer.buffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)
        commandBuffer.end()
        commandBuffer.submit(queue)
        vkQueueWaitIdle(queue)
    }

    fun destroy(logicalDevice: LogicalDevice) {
        vkDestroyImage(logicalDevice.device, texture, null)
        vkDestroySampler(logicalDevice.device, textureSampler, null)
    }
}
