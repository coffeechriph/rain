package rain.api.gfx

import org.joml.Vector3f
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

interface ResourceFactory {
    // TODO: Look into creating vertex buffers with selectable attributes (pos,color,uv....)
    fun createVertexBuffer(vertices: FloatArray, state: VertexBufferState, attributes: Array<VertexAttribute> = arrayOf(VertexAttribute(0, 2), VertexAttribute(1, 2))): VertexBuffer
    fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material
    fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, color: Vector3f): Material
    fun loadTexture2d(textureFile: String, filter: TextureFilter): Texture2d
    fun createTexture2d(imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter): Texture2d
}
