package example.roguelike.Entity

import example.roguelike.Level.Level
import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene

class Player : Entity() {
    private var leftActive = false
    private var rightActive = false
    private var upActive = false
    private var downActive = false

    var playerMovedCell = false
    var cellX = 0
        private set
    var cellY = 0
        private set
    var map = IntArray(0)
    var mapWidth: Int = 0
    var mapHeight: Int = 0
    var width: Int = 0
    var height: Int = 0
    var tileWidth: Int = 0
    var maxCellX = 0
    var maxCellY = 0
    lateinit var inventory: Inventory
    lateinit var attack: Attack
    lateinit var transform: Transform
    lateinit var sprite: Sprite
    lateinit var animator: Animator
    lateinit var collider: Collider

    var health = 100
    var stamina = 1
    var strength = 1
    var agility = 1
    var luck = 1
    lateinit var level: Level

    var baseHealth = 100
    var healthDamaged = 0
        set (value) {
            field = value
            inCombatCooldown = 100
            inCombat = true
            regenHealthTimeout = 0
        }
    val baseStamina = 5
    val baseStrength = 5
    val baseAgility = 5
    val baseLuck = 5

    var currentLevel = 1
    private var regenHealthTimeout = 0
    private var inCombatCooldown = 0
    private var inCombat = false

    fun setPosition(pos: Vector2i) {
        cellX = pos.x / 1280
        cellY = pos.y / 768
        collider.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%768)
        playerMovedCell = true
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        sprite = system.findSpriteComponent(getId())!!
        transform = system.findTransformComponent(getId())!!
        animator = system.findAnimatorComponent(getId())!!
        collider = system.findColliderComponent(getId())!!
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

        setDirectionBasedOnInput(input)

        if (attack.isReady() && !inventory.visible) {
            if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
                attack.attack(Direction.LEFT)
            } else if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
                attack.attack(Direction.RIGHT)
            } else if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED) {
                attack.attack(Direction.UP)
            } else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED) {
                attack.attack(Direction.DOWN)
            }
        }

        var velX = 0.0f
        var velY = 0.0f
        if (leftActive) {
            velX -= 120.0f
        }

        if (rightActive) {
            velX += 120.0f
        }

        if (upActive) {
            velY -= 120.0f
        }

        if (downActive) {
            velY += 120.0f
        }

        if (velX < 0.0f) {
            animator.setAnimation("walk_left")
        }
        else if (velX > 0.0f) {
            animator.setAnimation("walk_right")
        }
        else if (velY < 0.0f) {
            animator.setAnimation("walk_up")
        }
        else if (velY > 0.0f) {
            animator.setAnimation("walk_down")
        }

        if (velX == 0.0f && velY == 0.0f) {
            animator.setAnimation("idle")
        }

        collider.setVelocity(velX, velY)

        keepPlayerWithinBorder<Player>()

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

    private fun setDirectionBasedOnInput(input: Input) {
        if (input.keyState(Input.Key.KEY_A) == Input.InputState.PRESSED) {
            leftActive = true
        } else if (input.keyState(Input.Key.KEY_A) == Input.InputState.RELEASED) {
            leftActive = false
        }

        if (input.keyState(Input.Key.KEY_D) == Input.InputState.PRESSED) {
            rightActive = true
        } else if (input.keyState(Input.Key.KEY_D) == Input.InputState.RELEASED) {
            rightActive = false
        }

        if (input.keyState(Input.Key.KEY_W) == Input.InputState.PRESSED) {
            upActive = true
        } else if (input.keyState(Input.Key.KEY_W) == Input.InputState.RELEASED) {
            upActive = false
        }

        if (input.keyState(Input.Key.KEY_S) == Input.InputState.PRESSED) {
            downActive = true
        } else if (input.keyState(Input.Key.KEY_S) == Input.InputState.RELEASED) {
            downActive = false
        }
    }

    // TODO: This method uses constant window dimensions
    private fun <T : Entity> keepPlayerWithinBorder() {
        if (collider.getPosition().x < 0) {
            if (cellX > 0) {
                collider.setPosition(1270.0f, collider.getPosition().y)

                playerMovedCell = true
                cellX -= 1
            }
        }
        else if (collider.getPosition().x > 1270) {
            if (cellX < level.maxCellX) {
                collider.setPosition(10.0f, collider.getPosition().y)

                playerMovedCell = true
                cellX += 1
            }
        }

        if (collider.getPosition().y < 0) {
            if (cellY > 0) {
                collider.setPosition(collider.getPosition().x, 758.0f)

                playerMovedCell = true
                cellY -= 1
            }
        }
        else if (collider.getPosition().y > 768) {
            if (cellY < level.maxCellY) {
                collider.setPosition(collider.getPosition().x, 10.0f)

                playerMovedCell = true
                cellY += 1
            }
        }
    }
}
