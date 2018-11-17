package example.roguelike.Entity

import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.scene.Scene
import java.util.*

class Krac: Enemy() {
    private var idleDir = 0

    override fun onCollision(entity: Entity) {
        val random = Random(System.currentTimeMillis())
        if (entity is Attack && entity.attacker is Player) {
            val player = entity.attacker as Player
            val baseDamage = player.strength * 1.5f
            val critChange = 0.05 * player.agility
            val critted = random.nextFloat() < critChange
            var damage = baseDamage * random.nextFloat()
            if (critted) {
                damage *= random.nextInt(4) + 1.5f
            }

            damage(damage.toInt())
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
    }
}
