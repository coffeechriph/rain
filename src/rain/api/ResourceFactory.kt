package rain.api

import org.joml.Vector3f

interface ResourceFactory {
    fun createMaterial(vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d, color: Vector3f): Material
    fun createTexture2d(textureFile: String, filter: TextureFilter): Texture2d
}