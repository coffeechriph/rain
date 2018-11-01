package rain.api

interface Renderer {
    fun create()
    fun render()
    fun setActiveCamera(camera: Camera)
    fun submitDraw(drawable: Drawable, vertexBuffer: VertexBuffer)
}
