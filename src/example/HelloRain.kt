package example

import rain.Api
import rain.Rain

fun main(args: Array<String>) {
    val api = Rain()
    api.create(1280,720,"Hello Rain!", Api.VULKAN)
    api.run()
}
