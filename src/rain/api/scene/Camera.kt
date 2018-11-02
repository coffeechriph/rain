package rain.api.scene

import org.joml.Matrix4f
import rain.api.entity.Transform

class Camera {
    var x: Float = 0.0f
    var y: Float = 0.0f
    var projection = Matrix4f()
        private set

    init {
        projection.setOrtho(0.0f, 1280.0f, 0.0f, 720.0f, -20.0f, 0.0f, true)
    }
}
