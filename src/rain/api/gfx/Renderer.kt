package rain.api.gfx

import org.joml.Vector2f
import rain.api.scene.Camera

internal interface Renderer {
    fun create()
    fun render()
    fun setActiveCamera(camera: Camera)
    fun getDepthRange(): Vector2f
}
