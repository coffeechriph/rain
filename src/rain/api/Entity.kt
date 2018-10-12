package rain.api

open class Entity {
    private var id: Long = -1L
        private set

    internal fun setId(id: Long) {
        this.id = id
    }

    fun getId(): Long {
        return id
    }

    open fun<T: Entity> init(scene: Scene, system: EntitySystem<T>){}
    open fun<T: Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float){}
}
