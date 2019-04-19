package rain.api.scene

import org.joml.Matrix4f
import org.joml.Vector2i

class Camera(depthRange: Float, val resolution: Vector2i) {
    var x: Float = 0.0f
    var y: Float = 0.0f
    var maxDepth = 1000.0f
        private set
    internal var projection = Matrix4f()
        private set
    internal var view = Matrix4f()

    init {
        maxDepth = depthRange
        projection.setOrtho(0.0f, resolution.x.toFloat(), 0.0f, resolution.y.toFloat(), -maxDepth,
                maxDepth, true)
    }
}
