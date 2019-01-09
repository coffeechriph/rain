package example.roguelike.Entity

import example.roguelike.Level.Level
import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene
import java.util.*

class Player : Entity() {
    class InputTimestamp(var direction: Direction, var time: Long)
    var playerMovedCell = false
    var cellX = 0
        private set
    var cellY = 0
        private set
    lateinit var inventory: Inventory
    lateinit var attack: Attack
    lateinit var transform: Transform
    lateinit var sprite: Sprite
    lateinit var animator: Animator

    var health = 107
    var stamina = 5
    var strength = 5
    var agility = 5
    var luck = 5
    lateinit var level: Level

    var baseHealth = 100
        private set
    var healthDamaged = 0
        set (value) {
            field = value
            inCombatCooldown = 100
            inCombat = true
            regenHealthTimeout = 0
        }
    var baseStamina = 5
        private set
    var baseStrength = 5
        private set
    var baseAgility = 5
        private set
    var baseLuck = 5
        private set

    var currentLevel = 0
    private var regenHealthTimeout = 0
    private var inCombatCooldown = 0
    private var inCombat = false

    var playerXpLevel = 0
        private set
    var xp = 0
        private set
    var xpUntilNextLevel = 100
        private set
    var facingDirection = Direction.DOWN
    var closeToEnemy = false
    var targetedEnemy = -1
        private set

    private val inputTimestamps = ArrayList<InputTimestamp>()
    private var lastDirection = Direction.DOWN
    private var dodgeMovement = false
    private var dodgeDirection = Direction.NONE
    private var dodgeTick = 0.0f
    private var dodgeTimeout = 0.0f
    private var stopMovement = 0.0f
    private var targetIndex = 0

    fun addXp(increase: Int) {
        this.xp += increase
        if (this.xp >= xpUntilNextLevel) {
            this.xp = 0
            playerXpLevel += 1
            xpUntilNextLevel += xpUntilNextLevel

            baseStamina = (baseStamina.toFloat() * 1.3f).toInt()
            baseStrength = (baseStrength.toFloat() * 1.2f).toInt()
            baseAgility = (baseAgility.toFloat() * 1.2f).toInt()
            baseLuck = (baseLuck.toFloat() * 1.1f).toInt()
        }
        inventory.updateEquippedItems()
    }

    fun targetEnemy(enemies: ArrayList<Enemy>) {
        if (enemies.size <= 0) {
            targetIndex = -1
            return
        }

        targetIndex %= enemies.size
        if (targetIndex >= 0 && targetIndex < enemies.size) {
            targetedEnemy = targetIndex
        }
        else if (targetIndex >= enemies.size) {
            targetIndex = enemies.size - 1
            targetedEnemy = targetIndex
        }
    }

    fun setPosition(pos: Vector2i) {
        cellX = pos.x / 1280
        cellY = pos.y / 768
        transform.x = pos.x.toFloat()%1280
        transform.y = pos.y.toFloat()%768
        playerMovedCell = true
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        sprite = system.findSpriteComponent(getId())!!
        transform = system.findTransformComponent(getId())!!
        animator = system.findAnimatorComponent(getId())!!
        transform.setScale(64.0f,64.0f)

        animator.addAnimation("idle", 0, 0, 0, 0.0f)
        animator.addAnimation("walk_down", 0, 4, 0, 4.0f)
        animator.addAnimation("walk_right", 0, 4, 1, 4.0f)
        animator.addAnimation("walk_left", 0, 4, 2, 4.0f)
        animator.addAnimation("walk_up", 0, 4, 3, 4.0f)
        animator.setAnimation("idle")

        attack = Attack(transform)
        attack.attacker = this
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        transform.z = 1.0f + transform.y * 0.001f

        if (!inventory.visible) {
            setDirectionBasedOnInput(input)

            if (input.keyState(Input.Key.KEY_TAB) == Input.InputState.PRESSED) {
                targetIndex++
            }

            if (attack.isReady()) {
                if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
                    attack.attack(facingDirection)
                    stopMovement = 0.1f
                }
            }
        }

        movement()

        if (input.keyState(Input.Key.KEY_I) == Input.InputState.PRESSED) {
            inventory.visible = !inventory.visible
        }

        if (inCombat) {
            if (inCombatCooldown > 0) {
                inCombatCooldown -= 1
            }
            else if (inCombatCooldown <= 0) {
                inCombat = false
                inCombatCooldown = 0
            }
        }
        else {
            if (healthDamaged > 0 && regenHealthTimeout == 0) {
                healthDamaged -= 4
                if (healthDamaged < 0) {
                    healthDamaged = 0
                }

                regenHealthTimeout = 80
                inventory.updateHealthText()
            }

            if (regenHealthTimeout > 0) {
                regenHealthTimeout -= 1
            }
        }
    }

    private fun movement() {
        if (!closeToEnemy) {
            facingDirection = lastDirection
        }

        // Delay before movement starts
        when (facingDirection) {
            Direction.LEFT -> animator.setAnimation("walk_left")
            Direction.RIGHT -> animator.setAnimation("walk_right")
            Direction.UP -> animator.setAnimation("walk_up")
            Direction.DOWN -> animator.setAnimation("walk_down")
        }

        if (facingDirection == Direction.NONE) {
            animator.setAnimation("idle")
        }

        if (dodgeTimeout > 0.0f) {
            dodgeTimeout -= 0.1f
        }
        else {
            dodgeTimeout = 0.0f
        }

        if (stopMovement > 0.0f) {
            stopMovement -= (1.0f / 60.0f)
            return
        }
        else {
            stopMovement = 0.0f
        }

        val speed = 100.0f * (1.0f / 60.0f)
        if (!dodgeMovement) {
            if (inputTimestamps.size <= 0) {
                return
            }

            var velX = 0.0f
            var velY = 0.0f
            lastDirection = (inputTimestamps.sortedBy { i -> i.time })[0].direction
            when (lastDirection) {
                Direction.LEFT -> velX -= speed
                Direction.RIGHT -> velX += speed
                Direction.UP -> velY -= speed
                Direction.DOWN -> velY += speed
                Direction.NONE -> {
                }
            }

            if (level.collides(transform.x + velX, transform.y, 32.0f, 32.0f)) {
                velX = 0.0f
            }

            if (level.collides(transform.x, transform.y + velY, 32.0f, 32.0f)) {
                velY = 0.0f
            }

            transform.x += velX
            transform.y += velY
            keepPlayerWithinBorder(velX, velY)
        }
        else {
            // Delay before movement starts
            if (dodgeTick > 0.32f) {
                var velX = 0.0f
                var velY = 0.0f
                when (dodgeDirection) {
                    Direction.LEFT -> velX -= speed * 2
                    Direction.RIGHT -> velX += speed * 2
                    Direction.UP -> velY -= speed * 2
                    Direction.DOWN -> velY += speed * 2
                }

                transform.x += velX
                transform.y += velY
                keepPlayerWithinBorder(velX, velY)
            }

            dodgeTick += (10.0f / 60.0f)
            if (dodgeTick >= 3.0f) {
                dodgeMovement = false
                dodgeTick = 0.0f
                dodgeDirection = Direction.NONE
                dodgeTimeout = 3.0f
            }
        }
    }

    private fun setDirectionBasedOnInput(input: Input) {
        if (!dodgeMovement && dodgeTimeout <= 0.0f) {
            if (input.keyState(Input.Key.KEY_Q) == Input.InputState.PRESSED) {
                dodgeMovement = true

                if (facingDirection == Direction.UP) {
                    dodgeDirection = Direction.LEFT
                }
                else if (facingDirection == Direction.DOWN) {
                    dodgeDirection = Direction.LEFT
                }
                else if (facingDirection == Direction.LEFT) {
                    dodgeDirection = Direction.UP
                }
                else if (facingDirection == Direction.RIGHT) {
                    dodgeDirection = Direction.UP
                }
            }
            else if (input.keyState(Input.Key.KEY_E) == Input.InputState.PRESSED) {
                dodgeMovement = true
                if (facingDirection == Direction.UP) {
                    dodgeDirection = Direction.RIGHT
                }
                else if (facingDirection == Direction.DOWN) {
                    dodgeDirection = Direction.RIGHT
                }
                else if (facingDirection == Direction.LEFT) {
                    dodgeDirection = Direction.DOWN
                }
                else if (facingDirection == Direction.RIGHT) {
                    dodgeDirection = Direction.DOWN
                }
            }
        }

        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.LEFT, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.LEFT)
        }

        if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.RIGHT, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.RIGHT)
        }

        if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.UP, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_UP) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.UP)
        }

        if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED) {
            inputTimestamps.add(InputTimestamp(Direction.DOWN, System.currentTimeMillis()))
        } else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.RELEASED) {
            removeInputDirection(Direction.DOWN)
        }
    }

    private fun removeInputDirection(dir: Direction) {
        for (i in inputTimestamps) {
            if (i.direction == dir) {
                inputTimestamps.remove(i)
                break
            }
        }
    }

    // TODO: This method uses constant window dimensions
    private fun keepPlayerWithinBorder(velX: Float, velY: Float) {
        if (transform.x < 0 && velX < 0.0f) {
            if (cellX > 0) {
                transform.x = 1280.0f

                playerMovedCell = true
                cellX -= 1
            }
        }
        else if (transform.x > 1280 && velX > 0.0f) {
            if (cellX < level.maxCellX) {
                transform.x = 0.0f

                playerMovedCell = true
                cellX += 1
            }
        }

        if (transform.y < 0 && velY < 0.0f) {
            if (cellY > 0) {
                transform.y = 768.0f

                playerMovedCell = true
                cellY -= 1
            }
        }
        else if (transform.y > 768 && velY > 0.0f) {
            if (cellY < level.maxCellY) {
                transform.y = 0.0f

                playerMovedCell = true
                cellY += 1
            }
        }
    }
}
