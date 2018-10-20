package rain.api

import org.joml.Matrix4f
import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import java.nio.ByteBuffer

data class SpriteComponent internal constructor(val entity: Long, val material: Material, val transform: Transform, val textureTileOffset: Vector2i): Drawable
() {
    private val modelMatrixBuffer = memAlloc(18*4)
    private val modelMatrix = Matrix4f()

    override fun getMaterial(): Material {
        return material
    }

    override fun getTransform(): Transform {
        return transform
    }

    override fun getStreamedUniformData(): ByteBuffer {
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
        return modelMatrixBuffer
    }

    internal var animationTime = 0.0f
    internal var animationIndex = 0
    internal var animation = Animation(0,0,0,0.0f)
        private set
    private var animations = HashMap<String, Animation>()
    var visible = true

    fun addAnimation(name: String, startFrame: Int, endFrame: Int, yPos: Int, speed: Float) {
        val animation = Animation(startFrame, endFrame, yPos, speed)
        animations[name] = animation
        this.animation = animation
        textureTileOffset.y = yPos
    }

    fun startAnimation(name: String) {
        val tmp = animations[name] ?: throw IllegalArgumentException("No animation named $name exists!")
        if (animation != tmp) {
            animationTime = 0.0f
            animationIndex = 0
            animation = tmp
            textureTileOffset.y = animation.yPos
            textureTileOffset.x = animation.startFrame
        }
    }
}
