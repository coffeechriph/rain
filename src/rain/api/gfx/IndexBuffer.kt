package rain.api.gfx

interface IndexBuffer {
    fun valid(): Boolean
    fun update(indices: IntArray)
}