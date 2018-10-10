package rain.api

import java.nio.ByteBuffer

// Generic class which gives the Renderer knowledge about
// interesting data when rendering.
// getStreamedUniformData is used to specify data on the GPU that changes every frame
// The Vulkan implementation would use push constants while the OpenGL implementation would use uniforms
abstract class Drawable {
    internal abstract fun getTransform(): Transform
    internal abstract fun getStreamedUniformData(): ByteBuffer
    internal abstract fun getMaterial(): Material
}