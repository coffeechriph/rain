package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2i
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.gfx.Material
import java.nio.ByteBuffer

// TODO: We probably want a Animation component which updates a sprites TextureTileOffset
// This would remove the overhead of animations for sprites that don't need it.
// Also make it clearer that you can easily change textureTileOffset if no animation is attached
data class Sprite internal constructor(val entity: Long, val material: Material, val transform: Transform, val textureTileOffset: Vector2i) {
    var visible = true

    // TODO: Color will be added to the source atm
    // Extend this to be able to specify different modes like ADD,SUB,MUL
    var color = Vector4f(0.0f, 0.0f, 0.0f, 0.0f)

    private val modelMatrixBuffer = memAlloc(22*4)
    private val modelMatrix = Matrix4f()

    fun getUniformData(): ByteBuffer {
        if (transform.updated) {
            modelMatrix.identity()
            modelMatrix.rotateZ(transform.rot)
            modelMatrix.translate(transform.x, transform.y, transform.z)
            modelMatrix.scale(transform.sx, transform.sy, 0.0f)
        }

        val buffer = modelMatrix.get(modelMatrixBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
        val ibuf = buffer.asFloatBuffer()
        ibuf.put(16, textureTileOffset.x.toFloat())
        ibuf.put(17, textureTileOffset.y.toFloat())
        ibuf.put(18, color.x)
        ibuf.put(19, color.y)
        ibuf.put(20, color.z)
        ibuf.put(21, color.w)
        return modelMatrixBuffer
    }
}
