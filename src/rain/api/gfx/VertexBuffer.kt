package rain.api.gfx

interface VertexBuffer {
    fun valid(): Boolean
    fun update(vertices: FloatArray)
}
