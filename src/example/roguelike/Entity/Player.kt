package example.roguelike.Entity

import rain.api.*

class Player : Entity() {
    private var acc = 0.0f
    private var vel = 0.0f
    private var xdir = Direction.NONE
    private var ydir = Direction.NONE
    var playerMovedCell = true
    var cellX = 0
        private set
    var cellY = 0
        private set

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = system.findTransformComponent(getId())!!
        val sprite = system.findSpriteComponent(getId())!!
        transform.transform.position.set(512.0f,512.0f)
        transform.transform.scale.set(64.0f,64.0f)

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

        setDirectionBasedOnInput(input)

        if (ydir != Direction.NONE || xdir != Direction.NONE) {
            vel = 200.0f * deltaTime * Math.max(sprite.animationTime, 0.5f)

            when (xdir) {
                Direction.LEFT -> {
                    if (cellX > 0 || transform.transform.position.x - vel > 0) {
                        transform.transform.position.x -= vel
                        sprite.startAnimation("walk_left")
                    }
                }
                Direction.RIGHT -> {
                    // TODO: Constant window width
                    if (cellX < 1024 || transform.transform.position.x + vel < 1280) {
                        transform.transform.position.x += vel
                        sprite.startAnimation("walk_right")
                    }
                }
            }

            when (ydir) {
                Direction.UP -> {
                    if (cellY > 0 || transform.transform.position.y - vel > 0) {
                        transform.transform.position.y -= vel
                        sprite.startAnimation("walk_up")
                    }
                }
                Direction.DOWN -> {
                    // TODO: Constant window height
                    if (cellY < 1024 || transform.transform.position.y + vel < 720) {
                        transform.transform.position.y += vel
                        sprite.startAnimation("walk_down")
                    }
                }
            }
        }
        else {
            vel = 0.0f
            acc = 0.0f
            sprite.startAnimation("idle")
        }

        keepPlayerWithinBorder(transform.transform)
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
