package rain.api

import org.joml.Vector2f

/*
    FIXME: Transform is only 2D, this needs to be fixed if we ever move to 3D
 */
data class TransformComponent internal constructor(val entity: Long) {
    val transform = Transform()
}
