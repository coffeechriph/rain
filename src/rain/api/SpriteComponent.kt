package rain.api

import org.joml.Vector2i

data class SpriteComponent internal constructor(val entity: Long, val material: Material, val transform: TransformComponent, val textureTileOffset: Vector2i) {
    internal var animationTime = 0.0f
    internal var animationIndex = 0
    internal var animation = Animation(0,0,0)
        private set
    private var animations = HashMap<String, Animation>()

    fun addAnimation(name: String, startFrame: Int, endFrame: Int, yPos: Int) {
        val animation = Animation(startFrame, endFrame, yPos)
        animations[name] = animation
        this.animation = animation
        textureTileOffset.y = animation.yPos
    }

    fun startAnimation(name: String) {
        animation = animations[name] ?: throw IllegalArgumentException("No animation named $name exists!")
        textureTileOffset.y = animation.yPos
    }
}
