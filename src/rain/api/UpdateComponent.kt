package rain.api

class UpdateComponent internal constructor(val entity: Long) {
    lateinit var update: (id: Long, system: EntitySystem, scene: Scene) -> Unit
}
