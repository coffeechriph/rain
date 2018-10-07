package rain.api

import org.joml.Vector3f
import java.nio.FloatBuffer

interface ResourceFactory {
    // TODO: Look into creating vertex buffers with selectable attributes (pos,color,uv....)
    fun createVertexBuffer(vertices: FloatArray): VertexBuffer
    fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material
    fun createTexture2d(textureFile: String, filter: TextureFilter): Texture2d
}