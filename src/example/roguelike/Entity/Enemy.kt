package example.roguelike.Entity

import org.joml.Random
import org.joml.Vector2i
import org.joml.Vector4f
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene
import kotlin.math.sin

open class Enemy(val random: Random, val player: Player) : Entity() {
    var cellX = 0
        private set
    var cellY = 0
        private set
    private var attackAnimation = 0.0f
    private var wasAttacked = false

    var strength = 1
    var agility = 1
    var health = 100

    var strengthFactor = 1.0f
    var agilityFactor = 1.0f
    var healthFactor = 1.0f

    var healthBar = HealthBar()
    var traversing = false
    var path = ArrayList<Vector2i>()
    var pathIndex = 0
    var traverseSleep = 0L
    var direction: Direction = Direction.NONE
    var lastPlayerAngle = 0.0f
    lateinit var transform: Transform
    lateinit var collider: Collider
    lateinit var sprite: Sprite
    lateinit var animator: Animator

    protected var attackTimeoutValue = 30
    protected var attackTimeout = 0
    var walkingSpeedFactor = 1.0f
        protected set
    var pushBack = 0
    var pushBackImmune = false
    var pushDirection = Vector2i(0,0)
    var lastX = -1
    var lastY = -1

    // Attack
    var attacking = false
        private set
    private var prepareAttack = 0.0f
    private var attackArea = Vector4f()

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        transform.setScale(80.0f, 80.0f)
        transform.z = 1.0f + pos.y%768 * 0.001f
        cellX = pos.x / 1280
        cellY = pos.y / 768
        collider.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%768)
        transform.x = collider.getPosition().x
        transform.y = collider.getPosition().y
    }

    fun damage(player: Player) {
        if (!wasAttacked) {
            wasAttacked = true
            attackAnimation = 0.0f

            val baseDamage = player.strength * 1.5f
            val critChange = Math.min((0.05f * player.agility - 0.005f * agility), 0.0f)
            val critted = random.nextFloat() < critChange
            var damage = baseDamage * random.nextFloat()
            if (critted) {
                damage *= random.nextInt(4) + 1.5f
            }

            health -= damage.toInt()
        }
    }

    fun attack() {
        if (attackTimeout == 0) {
            attacking = true
            attackTimeout = attackTimeoutValue

            val px = ((player.transform.x - transform.x) / 64).toInt()
            val py = ((player.transform.y - transform.y) / 64).toInt()
            when {
                px > 0 -> direction = Direction.RIGHT
                px < 0 -> direction = Direction.LEFT
                py > 0 -> direction = Direction.UP
                py < 0 -> direction = Direction.DOWN
            }
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
        if (attacking) {
            when (direction) {
                Direction.LEFT -> attackArea = Vector4f(transform.x - 72.0f, transform.y, 72.0f, 64.0f)
                Direction.RIGHT -> attackArea = Vector4f(transform.x, transform.y, 64.0f + 72.0f, 64.0f)
                Direction.UP -> attackArea = Vector4f(transform.x, transform.y - 72.0f, 64.0f, 72.0f)
                Direction.DOWN -> attackArea = Vector4f(transform.x, transform.y, 64.0f, 64.0f + 72.0f)
            }

            if (prepareAttack < 1.0f) {
                prepareAttack += 0.01f
            }
            else {
                prepareAttack = 0.0f
                attacking = false

                val baseDamage = strength * 1.5f
                val critChange = Math.min((0.05f * agility) - (0.005f * player.agility), 0.0f)
                val critted = random.nextFloat() < critChange
                var damage = baseDamage * random.nextFloat()
                if (critted) {
                    damage *= random.nextInt(4) + 1.5f
                }

                if (player.transform.x + 32.0f >= attackArea.x && player.transform.x <= attackArea.x + attackArea.z
                &&  player.transform.y + 32.0f >= attackArea.y && player.transform.y <= attackArea.y + attackArea.w) {
                    player.healthDamaged += Math.max(1, damage.toInt())
                    player.inventory.updateHealthText()
                }
            }
        }
        else {
            if (attackTimeout > 0) {
                attackTimeout -= 1
            }
        }
    }
}
