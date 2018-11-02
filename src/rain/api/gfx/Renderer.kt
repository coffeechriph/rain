package rain.api.gfx

import rain.api.scene.Camera

interface Renderer {
    fun create()
    fun render()
    fun setActiveCamera(camera: Camera)
    fun submitDraw(drawable: Drawable, vertexBuffer: VertexBuffer)
}
