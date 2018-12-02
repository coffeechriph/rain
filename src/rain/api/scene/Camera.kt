package rain.api.scene

import org.joml.Matrix4f
import org.joml.Vector2f

class Camera(depthRange: Vector2f) {
    var x: Float = 0.0f
    var y: Float = 0.0f
    var projection = Matrix4f()
        private set

    init {
        projection.setOrtho(0.0f, 1280.0f, 0.0f, 768.0f, -depthRange.y, depthRange.x, true)
    }
}
