package example.roguelike

import example.roguelike.Entity.Attack
import example.roguelike.Entity.HealthBar
import example.roguelike.Entity.Player
import example.roguelike.Level.Level
import org.joml.Vector3f
import rain.Rain
import rain.api.*

class Roguelike: Rain() {
    private lateinit var attackMaterial: Material
    private lateinit var mobMaterial: Material
    private lateinit var mobTexture: Texture2d
    private lateinit var healthMaterial: Material
    private var playerSystem = EntitySystem<Player>()
    private var attackSystem = EntitySystem<Attack>()
    private var healthBarSystem = EntitySystem<HealthBar>()
    private var player = Player()
    private var camera = Camera()
    private var level = Level()

    override fun init() {
        mobTexture = resourceFactory.createTexture2d("./data/textures/dwarf.png", TextureFilter.NEAREST)
        mobTexture.setTiledTexture(16,16)
        mobMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", mobTexture, Vector3f(1.0f, 1.0f, 1.0f))
        player = Player()
        playerSystem.newEntity(player)
                .attachTransformComponent()
                .attachSpriteComponent(mobMaterial)
                .build(scene)
        scene.addSystem(playerSystem)

        val attackTexture = resourceFactory.createTexture2d("./data/textures/attack.png", TextureFilter.NEAREST)
        attackTexture.setTiledTexture(16,16)
        attackMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", attackTexture, Vector3f(1.0f, 1.0f,
                1.0f))

        attackSystem = EntitySystem()
        attackSystem.newEntity(player.attack)
                .attachTransformComponent()
                .attachSpriteComponent(attackMaterial)
                .build(scene)
        scene.addSystem(attackSystem)

        val healthTexture = resourceFactory.createTexture2d("./data/textures/health.png", TextureFilter.NEAREST)
        healthMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", healthTexture, Vector3f(1.0f, 1.0f,
                1.0f))
        healthBarSystem = EntitySystem()
        scene.addSystem(healthBarSystem)

        // TODO: Constant window dimensions
        level.create(resourceFactory, scene, 8960 / 64, 5040/64, 1280 / 64, 720 / 64 + 1)
        level.build(resourceFactory, scene, 0, healthBarSystem, healthMaterial)
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
        player.tileWidth = 64
        player.setPosition(playerSystem, level.getFirstTilePos())
    }

    override fun update() {
        level.update(player, attackSystem, healthBarSystem)

        if (player.playerMovedCell) {
            level.switchCell(resourceFactory, player.cellX, player.cellY)
            player.playerMovedCell = false
        }
    }
}
