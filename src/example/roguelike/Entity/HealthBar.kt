package example.roguelike.Entity

import rain.api.*

class HealthBar: Entity() {
    lateinit var parentTransform: Transform
    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {

    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val transform = system.findTransformComponent(getId())!!
        transform.x = parentTransform.x
        transform.y = parentTransform.y - 32
        transform.z = 9.0f
    }
}