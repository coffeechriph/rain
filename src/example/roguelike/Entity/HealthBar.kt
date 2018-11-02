package example.roguelike.Entity

import rain.api.*
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Transform
import rain.api.scene.Scene

class HealthBar: Entity() {
    var parentTransform: Transform? = null
    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {

    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val transform = system.findTransformComponent(getId())!!
        transform.z = 9.0f

        if (parentTransform != null) {
            transform.x = parentTransform!!.x
            transform.y = parentTransform!!.y - 32
        }
    }
}
