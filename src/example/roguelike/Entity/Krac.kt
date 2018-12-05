package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene
import java.util.*

class Krac: Enemy() {
    private var idleDir = 0

    init {
        strengthFactor = 2.0f
        healthFactor = 1.5f
        agilityFactor = 0.1f
        walkingSpeedFactor = 0.75f
    }

    override fun onCollision(entity: Entity) {
        val random = Random(System.currentTimeMillis())
        if (entity is Attack && entity.attacker is Player) {
            val player = entity.attacker as Player
            damage(random, player)
        }
        else if (entity is Enemy && !entity.pushBackImmune) {
            val dx = (entity.transform.x - transform.x) * 400
            val dy = (entity.transform.y - transform.y) * 400
            entity.pushBack = 8
            entity.pushDirection = Vector2i(dx.toInt(),dy.toInt())
            entity.traversing = false
            entity.pushBackImmune = true
            pushBackImmune = true
        }
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        super.init(scene, system)

        // TODO: Should be able to animate on the Y axis as well
        animator.addAnimation("idle_up", 0, 0, 1, 1.0f)
        animator.addAnimation("idle_down", 0, 0, 0, 1.0f)
        animator.addAnimation("idle_left", 0, 0, 1, 1.0f)
        animator.addAnimation("idle_right", 0, 0, 0, 1.0f)
        animator.setAnimation("idle_up")

        attackTimeoutValue = 100
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        transform.z = 1.0f + transform.y * 0.001f

        if (animator.animationTime >= 1.0f) {
            idleDir = Random().nextInt(3)

            when(idleDir) {
                0 -> animator.setAnimation("idle_up")
                1 -> animator.setAnimation("idle_left")
                2 -> animator.setAnimation("idle_right")
                else -> animator.setAnimation("idle_down")
            }
        }

        handleDamage(transform)
        if (attackTimeout > 0) {
            attackTimeout -= 1
        }
    }
}
