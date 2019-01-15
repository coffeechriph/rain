package rain.api.gfx

import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

interface ResourceFactory {
    fun createIndexBuffer(indices: IntArray, state: VertexBufferState): IndexBuffer
    fun createTexelBuffer(initialSize: Long): TexelBuffer
    fun buildMaterial(): MaterialBuilder
    fun buildVertexBuffer(): VertexBufferBuilder
    fun loadTexture2d(name: String, textureFile: String, filter: TextureFilter): Texture2d
    fun createTexture2d(name: String, imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter): Texture2d
    fun deleteMaterial(name: String)
    fun deleteTexture2d(name: String)
    fun deleteVertexBuffer(vertexBuffer: VertexBuffer)
    fun deleteIndexBuffer(indexBuffer: IndexBuffer)
    fun getMaterial(name: String): Material
    fun getTexture2d(name: String): Texture2d
    fun clear()
}
