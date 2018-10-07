package rain.api

import org.joml.Vector2i

data class SpriteComponent internal constructor(val entity: Long, val material: Material, val transform: TransformComponent, val textureTileOffset: Vector2i)
