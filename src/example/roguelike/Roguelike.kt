package example.roguelike

import example.roguelike.Entity.Player
import org.joml.Vector3f
import rain.Api
import rain.Rain
import rain.api.*

class Roguelike: Rain() {
    private lateinit var mobMaterial: Material
    private lateinit var mobTexture: Texture2d
    private var playerSystem = EntitySystem<Player>()
    private var player = Player()
    private var camera = Camera()

    override fun init() {
        mobTexture = resourceFactory.createTexture2d("./data/textures/sprite.png", TextureFilter.NEAREST)
        mobTexture.setTiledTexture(32,32)
        mobMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", mobTexture, Vector3f(1.0f, 1.0f, 1.0f))
        player = Player()
        playerSystem.newEntity(player)
                .attachUpdateComponent()
                .attachTransformComponent()
                .attachSpriteComponent(mobMaterial)
                .build(scene)
        scene.addSystem(playerSystem)

        camera = Camera()
        scene.setActiveCamera(camera)
    }

    override fun update() {

    }
}


fun main(args: Array<String>) {
    val app = Roguelike()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
