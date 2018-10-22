package example.roguelike

import example.roguelike.Entity.Attack
import example.roguelike.Entity.HealthBar
import example.roguelike.Entity.Player
import example.roguelike.Level.Level
import org.joml.Vector3f
import rain.Rain
import rain.api.*
import rain.vulkan.transitionImageLayout

class Roguelike: Rain() {
    private lateinit var attackMaterial: Material
    private lateinit var mobMaterial: Material
    private lateinit var mobTexture: Texture2d
    private lateinit var healthMaterial: Material
    private lateinit var playerSystem: EntitySystem<Player>
    private lateinit var attackSystem: EntitySystem<Attack>
    private lateinit var healthBarSystem: EntitySystem<HealthBar>
    private var player = Player()
    private var miniPlayer = HealthBar()
    private var camera = Camera()
    private var level = Level()

    override fun init() {
        mobTexture = resourceFactory.createTexture2d("./data/textures/dwarf.png", TextureFilter.NEAREST)
        mobTexture.setTiledTexture(16,16)
        mobMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", mobTexture, Vector3f(1.0f, 1.0f, 1.0f))
        player = Player()
        playerSystem = EntitySystem(scene)
        playerSystem.newEntity(player)
                .attachTransformComponent()
                .attachSpriteComponent(mobMaterial)
                .attachBoxColliderComponent(width = 24.0f, height = 32.0f)
                .build()
        scene.addSystem(playerSystem)

        val attackTexture = resourceFactory.createTexture2d("./data/textures/attack.png", TextureFilter.NEAREST)
        attackTexture.setTiledTexture(16,16)
        attackMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", attackTexture, Vector3f(1.0f, 1.0f,
                1.0f))

        attackSystem = EntitySystem(scene)
        attackSystem.newEntity(player.attack)
                .attachTransformComponent()
                .attachSpriteComponent(attackMaterial)
                .attachBoxColliderComponent(0.0f, 0.0f, 32.0f, 32.0f)
                .build()
        scene.addSystem(attackSystem)

        val healthTexture = resourceFactory.createTexture2d("./data/textures/health.png", TextureFilter.NEAREST)
        healthMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", healthTexture, Vector3f(1.0f, 1.0f,
                1.0f))
        healthBarSystem = EntitySystem(scene)
        scene.addSystem(healthBarSystem)

        // TODO: Constant window dimensions
        level.create(resourceFactory, scene, 8960 / 64, 5040/64, 1280 / 64, 720 / 64 + 1)
        level.build(resourceFactory, scene, 0, healthBarSystem, healthMaterial)
        scene.addTilemap(level.backTilemap)
        scene.addTilemap(level.frontTilemap)
        scene.addTilemap(level.minimapTilemap)

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

        healthBarSystem.newEntity(miniPlayer)
                .attachTransformComponent()
                .attachSpriteComponent(healthMaterial)
                .build()
        val mpt = healthBarSystem.findTransformComponent(miniPlayer.getId())!!
        mpt.setScale(4.0f, 4.0f)
    }

    override fun update() {
        level.update(player, healthBarSystem)

        if (player.playerMovedCell) {
            level.switchCell(resourceFactory, player.cellX, player.cellY)
            player.playerMovedCell = false
        }

        val mpt = healthBarSystem.findTransformComponent(miniPlayer.getId())!!
        val playerTransform = playerSystem.findTransformComponent(player.getId())!!
        // We need to divide by 32 as each tile on minimap is 2px in size while the actual tiles are 64
        // to adjust the player position according to the minimap
        // We then take the cell index multiplied by the number of tiles per cell * 2 to adjust to minimap tile size
        val tx = playerTransform.x / 32 + player.cellX * level.width * 2
        val ty = playerTransform.y / 32 + player.cellY * level.height * 2
        mpt.setPosition(tx, ty, 12.0f)
    }
}
