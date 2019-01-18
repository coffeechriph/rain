package rain

fun main(args: Array<String>) {
    val app = Rain()
    app.create(1280,720,"Stress Test", rain.api.Api.VULKAN)
    app.run()
}
