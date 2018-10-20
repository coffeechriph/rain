package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.*

class Player : Entity() {
    private var vel = 0.0f
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
    private val playerWidth = 12.0f
    private val playerHeight = 4.0f

    fun setPosition(system: EntitySystem<Player>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.position.x = pos.x.toFloat()%1280
        transform.position.y = pos.y.toFloat()%720
        cellX = pos.x / 1280
        cellY = pos.y / 720
        playerMovedCell = true
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = system.findTransformComponent(getId())!!
        val sprite = system.findSpriteComponent(getId())!!
        transform.position.set(1200.0f,600.0f, 2.0f)
        transform.scale.set(96.0f,96.0f)

        sprite.addAnimation("idle", 0, 0, 3, 0.0f)
        sprite.addAnimation("walk_left", 0, 4, 0, 4.0f)
        sprite.addAnimation("walk_right", 0, 4, 1, 4.0f)
        sprite.addAnimation("walk_down", 0, 4, 2, 4.0f)
        sprite.addAnimation("walk_up", 0, 4, 3, 4.0f)
        sprite.startAnimation("idle")
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val sprite = system.findSpriteComponent(getId())!!
        val transform = system.findTransformComponent(getId())!!
        transform.position.z = 2.0f + transform.position.y * 0.001f

        setDirectionBasedOnInput(input)

        if (ydir != Direction.NONE || xdir != Direction.NONE) {
            vel = 200.0f * deltaTime * Math.max(sprite.animationTime, 0.5f)

            val pxl = transform.position.x - playerWidth
            val pxr = transform.position.x + playerWidth

            val pyl = transform.position.y - playerHeight
            val pyr = transform.position.y
            when (xdir) {
                Direction.LEFT -> {
                    if (cellX > 0 || transform.position.x - vel > 0) {
                        if (!willCollide((pxl-vel).toInt(), pyl.toInt(), map) &&
                            !willCollide((pxl-vel).toInt(), pyr.toInt(), map)) {
                            transform.position.x -= vel
                        }
                        sprite.startAnimation("walk_left")
                    }
                }
                Direction.RIGHT -> {
                    // TODO: Constant window width
                    if (cellX < 1024 || transform.position.x + vel < 1280) {
                        if (!willCollide((pxr+vel).toInt(), pyl.toInt(), map) &&
                            !willCollide((pxr+vel).toInt(), pyr.toInt(), map)) {
                            transform.position.x += vel
                        }
                        sprite.startAnimation("walk_right")
                    }
                }
            }

            when (ydir) {
                Direction.UP -> {
                    if (cellY > 0 || transform.position.y - vel > 0) {
                        if (!willCollide(pxl.toInt(), (pyl - vel).toInt(), map) &&
                            !willCollide(pxr.toInt(), (pyl - vel).toInt(), map)) {
                            transform.position.y -= vel
                        }
                        sprite.startAnimation("walk_up")
                    }
                }
                Direction.DOWN -> {
                    // TODO: Constant window height
                    if (cellY < 1024 || transform.position.y + vel < 720) {
                        if (!willCollide(pxl.toInt(), (pyr + vel).toInt(), map) &&
                            !willCollide(pxr.toInt(), (pyr + vel).toInt(), map)) {
                            transform.position.y += vel
                        }
                        sprite.startAnimation("walk_down")
                    }
                }
            }
        }
        else {
            vel = 0.0f
            sprite.startAnimation("idle")
        }

        keepPlayerWithinBorder(transform)
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

    private fun willCollide(x: Int, y: Int, map: IntArray): Boolean {
        val cx = (cellX * width) + (x / tileWidth)
        val cy = (cellY * height) + (y / tileWidth)
        if (map[cx + cy*mapWidth] == 1) {
            return true
        }

        return false
    }

    // TODO: This method uses constant window dimensions
    private fun keepPlayerWithinBorder(transform: Transform) {
        if (transform.position.x < 0) {
            if (cellX > 0) {
                transform.position.x = 1280.0f
                playerMovedCell = true
                cellX -= 1
            }
        }
        else if (transform.position.x > 1280.0f) {
            // TODO: Make this a variable that can be randomly picked depending on level size
            if (cellX < 1024) {
                transform.position.x = 0.0f
                playerMovedCell = true
                cellX += 1
            }
        }

        if (transform.position.y < 0) {
            if (cellY > 0) {
                transform.position.y = 720.0f
                playerMovedCell = true
                cellY -= 1
            }
        }
        else if (transform.position.y > 720.0f) {
            // TODO: Make this a variable that can be randomly picked depending on level size
            if (cellY < 1024) {
                transform.position.y = 0.0f
                playerMovedCell = true
                cellY += 1
            }
        }
    }
}
