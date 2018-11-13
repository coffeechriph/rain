package rain.api.gfx

import org.joml.Vector2f
import rain.api.scene.Camera

interface Renderer {
    fun create()
    fun render()
    fun setActiveCamera(camera: Camera)
    fun submitDraw(drawable: Drawable, vertexBuffer: VertexBuffer)
    fun getDepthRange(): Vector2f
}
