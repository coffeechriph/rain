package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.*
import rain.api.entity.*
import rain.api.scene.Scene
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
    var traversing = false
    var path = ArrayList<Vector2i>()
    var pathIndex = 0
    var traverseSleep = 0L
    var lastPlayerAngle = 0.0f
    lateinit var transform: Transform
    lateinit var collider: Collider
    lateinit var sprite: Sprite
    lateinit var animator: Animator

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        collider.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%752)
        transform.setScale(80.0f, 80.0f)
        transform.z = 1.0f + pos.y%752 * 0.001f
        cellX = pos.x / 1280
        cellY = pos.y / 752
    }

    fun damage(dmg: Int) {
        if (!wasAttacked) {
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
        transform = system.findTransformComponent(getId())!!
        collider = system.findColliderComponent(getId())!!
        sprite = system.findSpriteComponent(getId())!!
        animator = system.findAnimatorComponent(getId())!!
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
    }
}
