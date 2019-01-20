package rain.api.gfx

import java.nio.ByteBuffer

interface VertexBuffer {
    fun valid(): Boolean
    fun update(vertices: ByteBuffer)
}
