package rain.api.scene

import org.joml.Matrix4f
import org.joml.Vector2f

class Camera(depthRange: Float, resolution: Vector2f) {
    var x: Float = 0.0f
    var y: Float = 0.0f
    var projection = Matrix4f()
        private set
    var maxDepth = 1000.0f
        private set
    var view = Matrix4f()

    init {
        maxDepth = depthRange
        projection.setOrtho(0.0f, resolution.x, 0.0f, resolution.y, -maxDepth, maxDepth, true)
    }
}
