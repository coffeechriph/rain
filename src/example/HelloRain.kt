package example

import rain.Api
import rain.Rain

class HelloRain: Rain() {
    override fun init() {

    }

    override fun update() {

    }
}
fun main(args: Array<String>) {
    val app = HelloRain()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
