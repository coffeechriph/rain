package rain.api.gfx

import org.joml.Vector2f
import rain.api.entity.Transform
import rain.api.scene.Camera
import java.nio.ByteBuffer

interface Renderer {
    fun create()
    fun render()
    fun setActiveCamera(camera: Camera)
    fun submitDraw(drawable: Drawable)
    fun getDepthRange(): Vector2f
}
