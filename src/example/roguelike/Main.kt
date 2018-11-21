package example.roguelike

import org.joml.Vector2i
import rain.api.Api
import rain.api.scene.NavMesh
import java.util.*

fun main(args: Array<String>) {
    val app = Roguelike()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
