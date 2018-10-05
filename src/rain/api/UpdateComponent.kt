package rain.api

class UpdateComponent internal constructor(val entity: Long) {
    lateinit var update: (scene: Scene) -> Unit
}
