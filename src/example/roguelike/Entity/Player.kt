package example.roguelike.Entity

import rain.api.Entity
import rain.api.EntitySystem
import rain.api.Input
import rain.api.Scene

class Player : Entity() {
    private var acc = 0.0f
    private var vel = 0.0f
    private var xdir = Direction.NONE
    private var ydir = Direction.NONE

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = system.findTransformComponent(id)!!
        val sprite = system.findSpriteComponent(id)!!
        transform.transform.position.set(512.0f,512.0f)
        transform.transform.scale.set(64.0f,64.0f)

        sprite.addAnimation("idle", 0, 0, 0)
        sprite.addAnimation("walk_down", 0, 3, 0)
        sprite.startAnimation("idle")
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val sprite = system.findSpriteComponent(id)!!
        val transform = system.findTransformComponent(id)!!

        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED || input.keyState(Input.Key.KEY_LEFT) == Input.InputState.DOWN) {
            xdir = Direction.LEFT
        }
        else if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.RELEASED) {
            xdir = Direction.NONE
        }

        if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED || input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.DOWN) {
            xdir = Direction.RIGHT
        }
        else if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.RELEASED) {
            xdir = Direction.NONE
        }

        if (input.keyState(Input.Key.KEY_UP) == Input.InputState.PRESSED || input.keyState(Input.Key.KEY_UP) == Input.InputState.DOWN) {
            ydir = Direction.UP
        }
        else if (input.keyState(Input.Key.KEY_UP) == Input.InputState.RELEASED) {
            ydir = Direction.NONE
        }

        if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.PRESSED || input.keyState(Input.Key.KEY_DOWN) == Input.InputState.DOWN) {
            ydir = Direction.DOWN
            sprite.startAnimation("walk_down")
        }
        else if (input.keyState(Input.Key.KEY_DOWN) == Input.InputState.RELEASED) {
            ydir = Direction.NONE
        }

        if (ydir != Direction.NONE || xdir != Direction.NONE) {
            vel = 100.0f * deltaTime

            when(xdir) {
                Direction.LEFT -> transform.transform.position.x -= vel
                Direction.RIGHT -> transform.transform.position.x += vel
            }

            when(ydir) {
                Direction.UP -> transform.transform.position.y -= vel
                Direction.DOWN -> transform.transform.position.y += vel
            }
        }
        else {
            vel = 0.0f
            acc = 0.0f
            sprite.startAnimation("idle")
        }
    }
}
