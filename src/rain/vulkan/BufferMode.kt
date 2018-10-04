package rain.vulkan

enum class BufferMode(internal val mode: Int) {
    SINGLE_BUFFER(1),
    ONE_PER_SWAPCHAIN(2);
}