package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene

class Player() : Entity() {
    private var left_active = false
    private var right_active = false
    private var up_active = false
    private var down_active = false

    var playerMovedCell = true
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

    val baseHealth = 100
    val baseStamina = 1
    val baseStrength = 1
    val baseAgility = 1
    val baseLuck = 1

    fun setPosition(pos: Vector2i) {
        collider.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%720)

        cellX = pos.x / 1280
        cellY = pos.y / 720
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

        if (attack.isReady()) {
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
        if (left_active) {
            velX -= 120.0f
        }

        if (right_active) {
            velX += 120.0f
        }

        if (up_active) {
            velY -= 120.0f
        }

        if (down_active) {
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

        keepPlayerWithinBorder(system)

        if (input.keyState(Input.Key.KEY_I) == Input.InputState.PRESSED) {
            inventory.visible = !inventory.visible
        }
    }

    private fun setDirectionBasedOnInput(input: Input) {
        if (input.keyState(Input.Key.KEY_A) == Input.InputState.PRESSED) {
            left_active = true
        } else if (input.keyState(Input.Key.KEY_A) == Input.InputState.RELEASED) {
            left_active = false
        }

        if (input.keyState(Input.Key.KEY_D) == Input.InputState.PRESSED) {
            right_active = true
        } else if (input.keyState(Input.Key.KEY_D) == Input.InputState.RELEASED) {
            right_active = false
        }

        if (input.keyState(Input.Key.KEY_W) == Input.InputState.PRESSED) {
            up_active = true
        } else if (input.keyState(Input.Key.KEY_W) == Input.InputState.RELEASED) {
            up_active = false
        }

        if (input.keyState(Input.Key.KEY_S) == Input.InputState.PRESSED) {
            down_active = true
        } else if (input.keyState(Input.Key.KEY_S) == Input.InputState.RELEASED) {
            down_active = false
        }
    }

    // TODO: This method uses constant window dimensions
    private fun <T : Entity> keepPlayerWithinBorder(system: EntitySystem<T>) {
        if (collider.getPosition().x < 0) {
            if (cellX > 0) {
                collider.setPosition(1270.0f, collider.getPosition().y)

                playerMovedCell = true
                cellX -= 1
            }
        }
        else if (collider.getPosition().x > 1280.0f) {
            // TODO: Make this a variable that can be randomly picked depending on level size
            if (cellX < 1024) {
                collider.setPosition(10.0f, collider.getPosition().y)

                playerMovedCell = true
                cellX += 1
            }
        }

        if (collider.getPosition().y < 0) {
            if (cellY > 0) {
                collider.setPosition(collider.getPosition().x, 710.0f)

                playerMovedCell = true
                cellY -= 1
            }
        }
        else if (collider.getPosition().y > 720.0f) {
            // TODO: Make this a variable that can be randomly picked depending on level size
            if (cellY < 1024) {
                collider.setPosition(collider.getPosition().x, 10.0f)

                playerMovedCell = true
                cellY += 1
            }
        }
    }
}
