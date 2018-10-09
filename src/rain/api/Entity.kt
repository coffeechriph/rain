package rain.api

open class Entity {
    protected var id: Long = -1L
        private set

    internal fun setId(id: Long) {
        this.id = id
    }

    open fun init(scene: Scene, system: EntitySystem<Entity>){}
    open fun update(scene: Scene, input: Input, system: EntitySystem<Entity>){}
}
