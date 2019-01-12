package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2i
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import java.nio.ByteBuffer

data class Sprite internal constructor(val entity: Long, val transform: Transform, val textureTileOffset: Vector2i) {
    var visible = true

    // TODO: Color will be added to the source atm
    // Extend this to be able to specify different modes like ADD,SUB,MUL
    var color = Vector4f(0.0f, 0.0f, 0.0f, 0.0f)

    private lateinit var byteBuffer: ByteBuffer
    private val modelMatrix = Matrix4f()

    fun getUniformData(): ByteBuffer {
        if (::byteBuffer.isInitialized) {
            memFree(byteBuffer)
        }
        byteBuffer = memAlloc(22*4)

        if (transform.updated) {
            modelMatrix.identity()
            modelMatrix.rotateZ(transform.rot)
            modelMatrix.translate(transform.x, transform.y, transform.z)
            modelMatrix.scale(transform.sx, transform.sy, 0.0f)
        }

        val buffer = modelMatrix.get(byteBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
        val ibuf = buffer.asFloatBuffer()
        ibuf.put(16, color.x)
        ibuf.put(17, color.y)
        ibuf.put(18, color.z)
        ibuf.put(19, color.w)
        ibuf.put(20, textureTileOffset.x.toFloat())
        ibuf.put(21, textureTileOffset.y.toFloat())
        return byteBuffer
    }
}
