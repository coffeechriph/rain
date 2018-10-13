package rain.api

import org.joml.Matrix4f

class Camera {
    var transform = Transform()
    var projection = Matrix4f()
        private set

    init {
        projection.setOrtho(0.0f, 1280.0f, 0.0f, 720.0f, -10.0f, 0.0f, true)
    }
}