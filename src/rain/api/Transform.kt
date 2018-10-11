package rain.api

import org.joml.Vector2f

data class Transform(var position: Vector2f = Vector2f(), var scale: Vector2f = Vector2f(1.0f,1.0f), var rotation: Float = 0.0f)