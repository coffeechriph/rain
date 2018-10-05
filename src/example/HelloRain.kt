package example

import org.joml.Vector3f
import rain.Api
import rain.Rain
import rain.api.*

class Player {
    fun init(scene: Scene) {
        println("Initializing the player")
    }

    fun update(scene: Scene) {
        println("Updating player");
    }
}
class HelloRain: Rain() {
    lateinit var basicMaterial: Material
    lateinit var playerEntitySystem: EntitySystem
    lateinit var player: Player
    override fun init() {
        basicMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", Texture2d(0, 0, 0, TextureFilter.NEAREST), Vector3f(1.0f,1.0f,1.0f))
        playerEntitySystem = EntitySystem()
        player = Player()
        playerEntitySystem.newEntity()
                .attachUpdateComponent {player.update(scene)}
                .attachTransformComponent()
                .attachSpriteComponent(basicMaterial)
                .build(scene)  {player.init(scene)}
        scene.registerSystem(playerEntitySystem)
    }

    override fun update() {

    }
}

fun main(args: Array<String>) {
    val app = HelloRain()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
