package rain.api

class UpdateComponent internal constructor(val entity: Long) {
    lateinit var update: (scene: Scene, input: Input, transform: TransformComponent, sprite: SpriteComponent) -> Unit
}
