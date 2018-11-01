package rain.api

import org.joml.Vector2i
import rain.assertion

class Animator internal constructor(val entityId: Long, val textureTileOffset: Vector2i) {
    internal var animationTime = 0.0f
    internal var animationIndex = 0
    internal var animation = Animation(0,0,0,0.0f)
        private set
    private var animations = HashMap<String, Animation>()
    var animating = true

    fun addAnimation(name: String, startFrame: Int, endFrame: Int, yPos: Int, speed: Float) {
        animations[name] = Animation(startFrame, endFrame, yPos, speed)
    }

    fun setAnimation(name: String) {
        val anim = animations[name] ?: assertion("No animation with name $name could be found!")

        if (anim != animation) {
            animation = anim
            textureTileOffset.x = animation.startFrame
            textureTileOffset.y = animation.yPos
        }
    }
}
