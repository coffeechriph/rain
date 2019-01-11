package example.roguelike.Entity

import org.joml.Random
import org.joml.Vector2f
import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene
import kotlin.math.sin

open class Enemy(private val random: Random, val player: Player) : Entity() {
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
    protected var attackSpeed = 0.01f
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
    private var attackArea = Vector2f()
    var attackAreaVisual = Entity()
    lateinit var attackAreaVisualSprite: Sprite
    lateinit var attackAreaVisualTransform: Transform

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

            var px = (player.transform.x - transform.x)
            var py = (player.transform.y - transform.y)
            val ln = Math.sqrt((px*px+py*py).toDouble()).toFloat()
            px /= ln
            py /= ln
            val inCharge = Math.abs(px) > Math.abs(py)
            if (inCharge) {
                direction = if(px > 0){
                    Direction.RIGHT
                } else {
                    Direction.LEFT
                }
            }
            else {
                direction = if(py > 0) {
                    Direction.DOWN
                } else {
                    Direction.UP
                }
            }

            when (direction) {
                Direction.LEFT -> attackArea = Vector2f(-128.0f, 48.0f)
                Direction.RIGHT -> attackArea = Vector2f(128.0f, 48.0f)
                Direction.UP -> attackArea = Vector2f(48.0f, -128.0f)
                Direction.DOWN -> attackArea = Vector2f(48.0f, 128.0f)
            }

            attackAreaVisualTransform.x = transform.x
            attackAreaVisualTransform.y = transform.y
            attackAreaVisualTransform.z = 1.1f
            attackAreaVisualSprite.visible = true

            if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                attackAreaVisualTransform.sy = attackArea.y
                attackAreaVisualTransform.sx = 0.0f
            }
            else if (direction == Direction.UP || direction == Direction.DOWN) {
                attackAreaVisualTransform.sy = 0.0f
                attackAreaVisualTransform.sx = attackArea.x
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
        if (health <= 0) {
            attacking = false
        }

        if (attacking) {
            if (prepareAttack < 1.0f) {
                if (direction == Direction.LEFT) {
                    attackAreaVisualTransform.sx = attackArea.x * prepareAttack
                    attackAreaVisualTransform.x = transform.x + (attackArea.x * prepareAttack) * 0.5f
                }
                else if (direction == Direction.RIGHT) {
                    attackAreaVisualTransform.sx = attackArea.x * prepareAttack
                    attackAreaVisualTransform.x = transform.x + (attackArea.x * prepareAttack) * 0.5f
                }
                else if (direction == Direction.UP) {
                    attackAreaVisualTransform.sy = attackArea.y * prepareAttack
                    attackAreaVisualTransform.y = transform.y + (attackArea.y * prepareAttack) * 0.5f
                }
                else if (direction == Direction.DOWN) {
                    attackAreaVisualTransform.sy = attackArea.y * prepareAttack
                    attackAreaVisualTransform.y = transform.y + (attackArea.y * prepareAttack) * 0.5f
                }

                prepareAttack += attackSpeed
            }
            else {
                prepareAttack = 0.0f
                attacking = false

                val baseDamage = strength * 3.0f
                val critChange = Math.min((0.075f * agility) - (0.005f * player.agility), 0.0f)
                val critted = random.nextFloat() < critChange
                var damage = baseDamage * (random.nextFloat() + 0.1f)
                if (critted) {
                    damage *= random.nextInt(4) + 2.0f
                }

                val ax = if (attackArea.x > 0.0f) { transform.x } else { transform.x + attackArea.x }
                val ay = if (attackArea.y > 0.0f) { transform.y } else { transform.y + attackArea.y }

                // Attacking in Y direction
                if (player.transform.x + 12.0f >= ax && player.transform.x - 12.0f <= ax + Math.abs(attackArea.x)
                 && player.transform.y + 12.0f >= ay && player.transform.y - 12.0f <= ay + Math.abs(attackArea.y)) {
                    player.damagePlayer(Math.max(1, damage.toInt()), transform.x, transform.y)
                    player.inventory.updateHealthText()
                }
            }
        }
        else {
            attackAreaVisualSprite.visible = false
            if (attackTimeout > 0) {
                attackTimeout -= 1
            }
        }
    }
}
