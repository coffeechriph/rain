package rain.api.gfx

import rain.api.entity.Transform
import java.nio.ByteBuffer

data class Drawable(val transform: Transform, val material: Material, val uniformData: ByteBuffer, val vertexBuffer: VertexBuffer, val indexBuffer: IndexBuffer?)
