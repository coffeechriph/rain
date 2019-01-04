package rain.vulkan

import org.lwjgl.stb.STBImage.stbi_load
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.util.vma.Vma
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.assertion
import rain.log
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer

internal class VulkanTexture2d(val id: Long, val vk: Vk, val resourceFactory: VulkanResourceFactory): Texture2d {
    var texture: Long = 0
        private set
    var allocation: Long = 0
        private set
    var textureView: Long = 0
        private set
    var textureSampler: Long = 0
        private set
    var isValid = false
        private set

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

    override fun valid(): Boolean {
        return isValid
    }

    fun load(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, commandBuffer: CommandPool.CommandBuffer, queue: Queue, filePath: String, filter: TextureFilter) {
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

        createImage(logicalDevice, memoryProperties, commandBuffer, queue, imageData, width.get(0), height.get(0), channels.get(0), filter)
    }

    fun createImage(logicalDevice: LogicalDevice, memoryProperties: VkPhysicalDeviceMemoryProperties, commandBuffer: CommandPool.CommandBuffer, queue: Queue, imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter) {
        val format = findTextureFormat(channels)
        val textureFilter = when(filter) {
            TextureFilter.NEAREST -> VK_FILTER_NEAREST
            TextureFilter.LINEAR -> VK_FILTER_LINEAR
        }

        log("Texture format: $format")

        val buffer = RawBuffer(commandBuffer, queue, resourceFactory)
        buffer.create(vk.vmaAllocator, imageData.remaining().toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_CPU_ONLY, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        val imageDataBuffer = MemoryUtil.memAlloc(imageData.remaining())
        imageDataBuffer.put(imageData)
        imageDataBuffer.flip()

        val pData = MemoryUtil.memAllocPointer(1)
        var err = vmaMapMemory(vk.vmaAllocator, buffer.allocation, pData)

        val data = pData.get(0)
        MemoryUtil.memFree(pData)
        if (err != VK10.VK_SUCCESS) {
            assertion("Failed to map image memory: " + VulkanResult(err))
        }

        MemoryUtil.memCopy(MemoryUtil.memAddress(imageDataBuffer), data, imageDataBuffer.remaining().toLong())
        MemoryUtil.memFree(imageDataBuffer)
        vmaUnmapMemory(vk.vmaAllocator, buffer.allocation)

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
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .flags(0)

            val extent = imageCreateInfo
                    .extent()

            extent.width(width)
                    .height(height)
                    .depth(1) // Depth must be 1 if type is VK_IMAGE_TYPE_2D

            val pAllocationCreateInfo = VmaAllocationCreateInfo.calloc()
                    .flags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)

            val textureImage = memAllocLong(1)
            val pAllocation = memAllocPointer(1)
            err = vmaCreateImage(vk.vmaAllocator, imageCreateInfo, pAllocationCreateInfo, textureImage, pAllocation, null)
            if (err != VK_SUCCESS) {
                assertion("Error creating image: ${VulkanResult(err)}")
            }

            val memoryRequirements = VkMemoryRequirements.callocStack()
            vkGetImageMemoryRequirements(logicalDevice.device, textureImage.get(0), memoryRequirements)

            transitionImageLayout(commandBuffer, queue.queue, textureImage.get(0), format, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
            copyBufferToImage(commandBuffer, queue.queue, buffer.buffer, textureImage.get(0), width, height)
            transitionImageLayout(commandBuffer, queue.queue, textureImage.get(0), format, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)

            texture = textureImage.get(0)
            allocation = pAllocation[0]

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

        this.isValid = true
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

    private fun copyBufferToImage(commandBuffer: CommandPool.CommandBuffer, queue: VkQueue, buffer: Long, image: Long, width: Int, height: Int) {
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

    fun invalidate() {
        isValid = false
    }
}
