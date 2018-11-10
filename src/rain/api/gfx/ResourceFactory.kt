package rain.api.gfx

import org.joml.Vector3f
import java.nio.ByteBuffer

interface ResourceFactory {
    // TODO: Look into creating vertex buffers with selectable attributes (pos,color,uv....)
    fun createVertexBuffer(vertices: FloatArray, state: VertexBufferState): VertexBuffer
    fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material
    fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Array<Texture2d>, color: Vector3f): Material
    fun loadTexture2d(textureFile: String, filter: TextureFilter): Texture2d
    fun createTexture2d(imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter): Texture2d
}
