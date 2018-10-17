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
        level.create(resourceFactory, 12800 / 32, 7200/32, 1280 / 32, 720 / 32 + 1)
        level.build(resourceFactory, 0)
        scene.addTilemap(level.backTilemap)
        scene.addTilemap(level.frontTilemap)

        camera = Camera()
        scene.setActiveCamera(camera)

        player.map = level.map
        player.width = level.width
        player.height = level.height
        player.mapWidth = level.mapWidth
        player.mapHeight = level.mapHeight
        player.maxCellX = level.maxCellX
        player.maxCellY = level.maxCellY
        player.tileWidth = 32
    }

    override fun update() {
        if (player.playerMovedCell) {
            level.switchCell(resourceFactory, player.cellX, player.cellY)
            player.playerMovedCell = false
        }
    }
}
