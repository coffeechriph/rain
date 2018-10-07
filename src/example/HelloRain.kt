package example

import org.joml.Vector3f
import rain.Api
import rain.Rain
import rain.api.*

class Player {
    var x = 0.0f
    var dir = -1

    fun init(id: Long, system: EntitySystem, scene: Scene) {
        println("Initializing the player")
    }

    fun update(id: Long, system: EntitySystem, scene: Scene) {
        val transform = system.findTransformComponent(id)!!
        transform.position.set(x, 0.0f)

        if (x <= -0.75f) {
            dir = 1
        }
        else if(x >= 0.75f) {
            dir = -1
        }
        x += dir * 0.0001f
    }
}

class HelloRain: Rain() {
    lateinit var basicMaterial: Material
    lateinit var basicTexture: Texture2d
    lateinit var playerEntitySystem: EntitySystem
    lateinit var player: Player
    override fun init() {
        basicTexture = resourceFactory.createTexture2d("./data/textures/town.png", TextureFilter.NEAREST)
        basicMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", basicTexture, Vector3f(1.0f,1.0f,1.0f))
        playerEntitySystem = EntitySystem()
        player = Player()
        playerEntitySystem.newEntity()
                .attachUpdateComponent(player::update)
                .attachTransformComponent()
                .attachSpriteComponent(basicMaterial)
                .build(scene, player::init)
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
