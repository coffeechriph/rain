package example.roguelike

import rain.Api

fun main(args: Array<String>) {
    val app = Roguelike()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
