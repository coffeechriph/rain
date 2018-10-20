package example.roguelike.Entity

import rain.api.*

/*
    TODO: Two things that would be nice to support in the engine to make things like these a bit easier.
    1. onCollision method which takes in two entities that collided.
    2. parent entity to allow this entities transform the be linked to the parent
 */
class Attack : Entity() {
    var parentTransform = Transform()
    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val transform = system.findTransformComponent(getId())!!
        transform.setPosition(1200.0f,600.0f, 9.0f)
        transform.setScale(96.0f,96.0f)
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val transform = system.findTransformComponent(getId())!!
        transform.setPosition(parentTransform.x, parentTransform.y, parentTransform.z + 0.01f)
    }
}