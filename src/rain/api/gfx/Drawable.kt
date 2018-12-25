package rain.api.gfx

import java.nio.ByteBuffer

data class Drawable(val material: Material, val uniformData: ByteBuffer, val vertexBuffer: VertexBuffer, val z: Float, val indexBuffer: IndexBuffer? = null)
