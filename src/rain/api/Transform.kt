package rain.api

import org.joml.Vector2f
import org.joml.Vector3f

data class Transform(var position: Vector3f = Vector3f(), var scale: Vector2f = Vector2f(1.0f,1.0f), var rotation: Float = 0.0f)