package rain.api.entity

import org.joml.Vector2i
import rain.assertion

class Animator {
    val textureTileOffset = Vector2i()
    internal var animationTime = 0.0f
    internal var animationIndex = 0
    internal var animation = Animation("none", 0, 0, 0, 0.0f)
        private set
    private var animations = HashMap<String, Animation>()
    var animating = true

    fun addAnimation(name: String, startFrame: Int, endFrame: Int, yPos: Int, speed: Float) {
        animations[name] = Animation(name, startFrame, endFrame, yPos, speed)
    }

    fun setAnimation(name: String) {
        val anim = animations[name] ?: assertion("No animation with name $name could be found!")

        if (anim != animation) {
            animation = anim
            textureTileOffset.x = animation.startFrame
            textureTileOffset.y = animation.yPos
        }
    }

    fun activeAnimation(): Animation {
       return animation
    }
}
