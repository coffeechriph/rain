package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.*

class Player : Entity() {
    private var xdir = Direction.NONE
    private var ydir = Direction.NONE
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
    val attack = Attack()

    fun setPosition(system: EntitySystem<Player>, pos: Vector2i) {
        val body = system.findColliderComponent(getId())!!
        body.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%720)

        cellX = pos.x / 1280
        cellY = pos.y / 720
        playerMovedCell = true
        attack.parentTransform = system.findTransformComponent(getId())!!
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = system.findTransformComponent(getId())!!
        val sprite = system.findSpriteComponent(getId())!!
        transform.setScale(64.0f,64.0f)

        sprite.addAnimation("idle", 0, 0, 0, 0.0f)
        sprite.addAnimation("walk_down", 0, 4, 0, 4.0f)
        sprite.addAnimation("walk_right", 0, 4, 1, 4.0f)
        sprite.addAnimation("walk_left", 0, 4, 2, 4.0f)
        sprite.addAnimation("walk_up", 0, 4, 3, 4.0f)
        sprite.startAnimation("idle")
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val sprite = system.findSpriteComponent(getId())!!
        val transform = system.findTransformComponent(getId())!!
        transform.z = 2.0f + transform.y * 0.001f

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

        // TODO: Take in input as queue where we use the latest key pressed
        // TODO: The current way of handling input fucks up the animation and feels clunky
        if (ydir != Direction.NONE || xdir != Direction.NONE) {
            when (xdir) {
                Direction.LEFT -> {
                    if (cellX > 0) {
                        val body = system.findColliderComponent(getId())!!
                        body.setVelocity(-200.0f, body.getVelocity().y)
                        sprite.startAnimation("walk_left")
                    }
                }
                Direction.RIGHT -> {
                    // TODO: Constant window width
                    if (cellX < 1024) {
                        val body = system.findColliderComponent(getId())!!
                        body.setVelocity(200.0f, body.getVelocity().y)
                        sprite.startAnimation("walk_right")
                    }
                }
            }

            when (ydir) {
                Direction.UP -> {
                    if (cellY > 0) {
                        val body = system.findColliderComponent(getId())!!
                        body.setVelocity(body.getVelocity().x, -200.0f)
                        sprite.startAnimation("walk_up")
                    }
                }
                Direction.DOWN -> {
                    // TODO: Constant window height
                    if (cellY < 1024) {
                        val body = system.findColliderComponent(getId())!!
                        body.setVelocity(body.getVelocity().x, 200.0f)
                        sprite.startAnimation("walk_down")
                    }
                }
            }
        }
        else {
            system.findColliderComponent(getId())!!.setVelocity(0.0f, 0.0f)
            sprite.startAnimation("idle")
        }

        keepPlayerWithinBorder(system)
    }

    private fun setDirectionBasedOnInput(input: Input) {
        if (input.keyState(Input.Key.KEY_A) == Input.InputState.PRESSED) {
            xdir = Direction.LEFT
        } else if (input.keyState(Input.Key.KEY_A) == Input.InputState.RELEASED) {
            xdir = Direction.NONE
        }

        if (input.keyState(Input.Key.KEY_D) == Input.InputState.PRESSED) {
            xdir = Direction.RIGHT
        } else if (input.keyState(Input.Key.KEY_D) == Input.InputState.RELEASED) {
            xdir = Direction.NONE
        }

        if (input.keyState(Input.Key.KEY_W) == Input.InputState.PRESSED) {
            ydir = Direction.UP
        } else if (input.keyState(Input.Key.KEY_W) == Input.InputState.RELEASED) {
            ydir = Direction.NONE
        }

        if (input.keyState(Input.Key.KEY_S) == Input.InputState.PRESSED) {
            ydir = Direction.DOWN
        } else if (input.keyState(Input.Key.KEY_S) == Input.InputState.RELEASED) {
            ydir = Direction.NONE
        }
    }

    // TODO: This method uses constant window dimensions
    private fun <T : Entity> keepPlayerWithinBorder(system: EntitySystem<T>) {
        val body = system.findColliderComponent(getId())!!
        if (body.getPosition().x < 0) {
            if (cellX > 0) {
                body.setPosition(1270.0f, body.getPosition().y)

                playerMovedCell = true
                cellX -= 1
            }
        }
        else if (body.getPosition().x > 1280.0f) {
            // TODO: Make this a variable that can be randomly picked depending on level size
            if (cellX < 1024) {
                body.setPosition(10.0f, body.getPosition().y)

                playerMovedCell = true
                cellX += 1
            }
        }

        if (body.getPosition().y < 0) {
            if (cellY > 0) {
                body.setPosition(body.getPosition().x, 710.0f)

                playerMovedCell = true
                cellY -= 1
            }
        }
        else if (body.getPosition().y > 720.0f) {
            // TODO: Make this a variable that can be randomly picked depending on level size
            if (cellY < 1024) {
                body.setPosition(body.getPosition().x, 10.0f)

                playerMovedCell = true
                cellY += 1
            }
        }
    }
}
