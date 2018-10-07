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

    fun update(scene: Scene, input: Input, transformComponent: TransformComponent, spriteComponent: SpriteComponent) {
        transformComponent.position.set(x, 0.0f)

        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
            println("Left was pressed!")
        }
        else if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.RELEASED) {
            println("Left was released!")
        }
    }
}

class HelloRain: Rain() {
    lateinit var basicMaterial: Material
    lateinit var basicTexture: Texture2d
    lateinit var playerEntitySystem: EntitySystem
    lateinit var player: Player
    override fun init() {
        basicTexture = resourceFactory.createTexture2d("./data/textures/town.png", TextureFilter.NEAREST)
        basicTexture.setTiledTexture(256, 256)
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
