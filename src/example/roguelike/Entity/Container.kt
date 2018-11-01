package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.Entity
import rain.api.EntitySystem
import rain.api.Input
import rain.api.Scene

class Container : Entity() {
    var health = 0
    var open = false
    var looted = false
    var cellX = 0
    var cellY = 0

    // TODO: Constant window size
    fun setPosition(system: EntitySystem<Container>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.z = 2.0f + transform.y * 0.001f
        transform.setScale(64.0f, 64.0f)
        val body = system.findColliderComponent(getId())!!
        body.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%720)
        cellX = pos.x / 1280
        cellY = pos.y / 720
    }

    override fun onCollision(entity: Entity) {
        if (!open && entity is Attack) {
            open = true
        }
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (open) {
            val animator = system.findAnimatorComponent(getId())!!
            animator.setAnimation("open")
        }
    }
}
