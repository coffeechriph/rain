package rain.api

import org.joml.Matrix4f
import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

data class SpriteComponent internal constructor(val entity: Long, val material: Material, val transform: Transform, val textureTileOffset: Vector2i): Drawable
() {
    override fun getMaterial(): Material {
        return material
    }

    override fun getTransform(): Transform {
        return transform
    }

    override fun getStreamedUniformData(): ByteBuffer {
        val modelMatrix = Matrix4f()
        modelMatrix.rotate(transform.rotation, 0.0f, 0.0f, 1.0f)
        modelMatrix.translate(transform.position.x, transform.position.y, transform.position.z)
        modelMatrix.scale(transform.scale.x, transform.scale.y, 0.0f)

        // TODO: We're reallocating this every frame
        val byteBuffer = MemoryUtil.memAlloc(18 * 4)
        val buffer = modelMatrix.get(byteBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
        val ibuf = buffer.asFloatBuffer()
        ibuf.put(16, textureTileOffset.x.toFloat())
        ibuf.put(17, textureTileOffset.y.toFloat())
        return byteBuffer
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
            animation = tmp
            textureTileOffset.y = animation.yPos
        }
    }
}
