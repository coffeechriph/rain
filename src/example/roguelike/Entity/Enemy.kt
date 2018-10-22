package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.*
import kotlin.math.sin

open class Enemy : Entity() {
    lateinit var player: Player

    var cellX = 0
        private set
    var cellY = 0
        private set
    private var attackAnimation = 0.0f
    private var wasAttacked = false
    var health = 100
        private set
    var healthBar = HealthBar()

    // TODO: Constant window size
    fun setPosition(system: EntitySystem<Enemy>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.z = 2.0f + transform.y * 0.001f
        transform.setScale(96.0f, 96.0f)
        val body = system.findColliderComponent(getId())!!
        body.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%720)
        cellX = pos.x / 1280
        cellY = pos.y / 720
    }

    fun damage(dmg: Int) {
        if (wasAttacked == false) {
            wasAttacked = true
            attackAnimation = 0.0f
            health -= dmg
        }
    }

    protected fun handleDamage(transform: Transform) {
        if (wasAttacked) {
            transform.y = transform.y + sin(attackAnimation * 100.0f) * 2.0f
            attackAnimation += 0.075f

            if (attackAnimation >= 1.0f) {
                wasAttacked = false
                attackAnimation = 0.0f
            }
        }
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {

    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {

    }
}
