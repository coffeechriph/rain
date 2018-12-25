package rain.api.gfx

import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

interface ResourceFactory {
    // TODO: Look into creating vertex buffers with selectable attributes (pos,color,uv....)
    fun createVertexBuffer(vertices: FloatArray, state: VertexBufferState, attributes: Array<VertexAttribute> = arrayOf(VertexAttribute(0, 2), VertexAttribute(1, 2))): VertexBuffer
    fun createIndexBuffer(indices: IntArray, state: VertexBufferState): IndexBuffer
    fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d?, depthWriteEnabled: Boolean = true): Material
    fun createMaterial(name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, depthWriteEnabled: Boolean = true): Material
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
