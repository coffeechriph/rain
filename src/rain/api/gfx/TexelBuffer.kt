package rain.api.gfx

import java.nio.ByteBuffer

interface TexelBuffer {
    fun update(data: ByteBuffer)
    fun getData(): ByteBuffer
}