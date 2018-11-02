package rain.api.entity

import rain.api.Input
import rain.api.scene.Scene

open class Entity {
    private var id: Long = -1L

    internal fun setId(id: Long) {
        this.id = id
    }

    fun getId(): Long {
        return id
    }

    open fun<T: Entity> init(scene: Scene, system: EntitySystem<T>){}
    open fun<T: Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float){}
    open fun onCollision(entity: Entity) {}
}
