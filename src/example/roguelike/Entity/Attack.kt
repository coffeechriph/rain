package example.roguelike.Entity

import rain.api.*

/*
    TODO: Two things that would be nice to support in the engine to make things like these a bit easier.
    1. onCollision method which takes in two entities that collided.
    2. parent entity to allow this entities transform the be linked to the parent
 */
class Attack : Entity() {
    var parentTransform = Transform()
    private var active = false
    private var direction = Direction.DOWN
    private var activeTime = 0

    fun attack(direction: Direction) {
        active = true
        this.direction = direction
    }

    fun isReady(): Boolean {
        return !active;
    }

    fun isActive(): Boolean {
        return active
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = system.findTransformComponent(getId())!!
        transform.setPosition(1200.0f,600.0f, 9.0f)
        transform.setScale(96.0f,96.0f)

        val sprite = system.findSpriteComponent(getId())!!
        sprite.addAnimation("down", 0, 0, 0, 0.0f)
        sprite.addAnimation("right", 1, 1, 0, 0.0f)
        sprite.addAnimation("up", 2, 2, 0, 0.0f)
        sprite.addAnimation("left", 3, 3, 0, 0.0f)

        // TODO: A problem here was that I had to add a idle animation for the LEFT
        // animation to actually trigger due to how it works in the SpriteComponent
        sprite.addAnimation("idle", 0, 0, 0, 0.0f)
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val sprite = system.findSpriteComponent(getId())!!

        if (active) {
            val transform = system.findTransformComponent(getId())!!

            when(direction) {
                Direction.LEFT -> {
                    transform.setPosition(parentTransform.x - 32, parentTransform.y, parentTransform.z + 0.01f)
                    sprite.startAnimation("left")
                }
                Direction.RIGHT -> {
                    transform.setPosition(parentTransform.x + 32, parentTransform.y, parentTransform.z + 0.01f)
                    sprite.startAnimation("right")
                }
                Direction.UP -> {
                    transform.setPosition(parentTransform.x, parentTransform.y - 32, parentTransform.z + 0.01f)
                    sprite.startAnimation("up")
                }
                Direction.DOWN -> {
                    transform.setPosition(parentTransform.x, parentTransform.y + 32, parentTransform.z + 0.01f)
                    sprite.startAnimation("down")
                }
            }

            sprite.visible = true
            activeTime++
            if (activeTime > 10) {
                active = false
                activeTime = 0
            }
        }
        else {
            sprite.visible = false
        }
    }
}