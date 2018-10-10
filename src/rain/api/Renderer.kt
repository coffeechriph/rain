package rain.api

import org.joml.Vector2i

/*
    Renderer interface implemented for different APIs and created once an
    API is chosen.
 */
interface Renderer {
    fun create()
    fun render()
    fun setActiveCamera(camera: Camera)
    fun submitDraw(drawable: Drawable, vertexBuffer: VertexBuffer)
}
