package example.roguelike

import rain.api.Api

fun main(args: Array<String>) {
    val app = Roguelike()
    app.create(1280,768,"Hello Rain!", Api.VULKAN)
    app.run()
}
