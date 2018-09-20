package rain.api

import org.joml.Vector2f

/*
    FIXME: Transform is only 2D, this needs to be fixed if we ever move to 3D
 */
data class TransformComponent internal constructor(val entity: Long) {
    var position: Vector2f = Vector2f(0.0f,0.0f)
    var scale: Vector2f = Vector2f(1.0f, 1.0f)
    var rotation: Float = 0.0f
}
