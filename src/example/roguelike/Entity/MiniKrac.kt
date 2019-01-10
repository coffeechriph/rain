package example.roguelike.Entity

import org.joml.Random
import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene

class MiniKrac(random: Random, player: Player): Enemy(random, player) {
    init {
        strengthFactor = 0.65f
        healthFactor = 0.65f
        agilityFactor = 2.0f
        walkingSpeedFactor = 1.0f
        attackSpeed = 0.013f
    }

    override fun onCollision(entity: Entity) {
        if (entity is Attack && entity.attacker is Player) {
            val player = entity.attacker as Player
            damage(player)
        }
        else if (entity is Enemy && !entity.pushBackImmune) {
            val dx = (entity.transform.x - transform.x) * 0.1f
            val dy = (entity.transform.y - transform.y) * 0.1f
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
        animator.addAnimation("idle_up", 4, 4, 1, 1.0f)
        animator.addAnimation("idle_down", 4, 4, 0, 1.0f)
        animator.addAnimation("idle_left", 4, 4, 1, 1.0f)
        animator.addAnimation("idle_right", 4, 4, 0, 1.0f)
        animator.addAnimation("walk_down", 4, 8, 0, 3.5f)
        animator.addAnimation("walk_up", 4, 8, 1, 3.5f)
        animator.setAnimation("idle_up")

        attackTimeoutValue = 30
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        super.update(scene, input, system, deltaTime)

        transform.z = 1.0f + transform.y * 0.001f
        handleDamage(transform)
    }
}
