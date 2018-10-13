package example.roguelike

import example.roguelike.Entity.Player
import example.roguelike.Level.Level
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
    private var level = Level()

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

        // TODO: Constant window dimensions
        level.create(resourceFactory, 1280 / 16, 720 / 16)
        level.build(resourceFactory, 0)
        scene.addTilemap(level.backTilemap)
        scene.addTilemap(level.frontTilemap)

        camera = Camera()
        scene.setActiveCamera(camera)
    }

    override fun update() {
        if (player.playerMovedCell) {
            level.build(resourceFactory, (player.cellX+player.cellY*1024).toLong())
            player.playerMovedCell = false
        }

        if (input.keyState(Input.Key.KEY_1) == Input.InputState.PRESSED) {
            level.backTilemap.getTransform().position.set(0.0f, 0.0f, level.backTilemap.getTransform().position.z + 0.5f)
            println(level.backTilemap.getTransform().position.z)
        }
    }
}


fun main(args: Array<String>) {
    val app = Roguelike()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
