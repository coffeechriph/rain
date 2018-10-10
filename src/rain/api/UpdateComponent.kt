package rain.api

class UpdateComponent<T: Entity> internal constructor(val entityHandler: Long, val entity: T) {
    fun update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        entity.update(scene, input, system, deltaTime)
    }
}
