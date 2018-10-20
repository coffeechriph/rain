package example.roguelike.Entity

import rain.api.Entity
import rain.api.EntitySystem
import rain.api.Input
import rain.api.Scene
import java.util.*

class Krac: Enemy() {
    private var idleDir = 0
    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val sprite = system.findSpriteComponent(getId())!!

        // TODO: Should be able to animate on the Y axis as well
        sprite.addAnimation("idle_up", 0, 0, 1, 1.0f)
        sprite.addAnimation("idle_down", 0, 0, 0, 1.0f)
        sprite.addAnimation("idle_left", 0, 0, 1, 1.0f)
        sprite.addAnimation("idle_right", 0, 0, 0, 1.0f)
        sprite.startAnimation("idle_up")
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val sprite = system.findSpriteComponent(getId())!!
        if (sprite.animationTime >= 1.0f) {
            idleDir = Random().nextInt(3)

            when(idleDir) {
                0 -> sprite.startAnimation("idle_up")
                1 -> sprite.startAnimation("idle_left")
                2 -> sprite.startAnimation("idle_right")
                else -> sprite.startAnimation("idle_down")
            }
        }

        handleDamage(system.findTransformComponent(getId())!!)
    }
}