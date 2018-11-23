package example.roguelike

import example.roguelike.Entity.Attack
import example.roguelike.Entity.HealthBar
import example.roguelike.Entity.Inventory
import example.roguelike.Entity.Player
import example.roguelike.Level.Level
import org.joml.Vector2f
import org.joml.Vector3f
import rain.Rain
import rain.api.entity.EntitySystem
import rain.api.gfx.Material
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.gui.Container
import rain.api.gui.Text
import rain.api.scene.Camera

class Roguelike: Rain() {
    private lateinit var attackMaterial: Material
    private lateinit var mobMaterial: Material
    private lateinit var mobTexture: Texture2d
    private lateinit var healthMaterial: Material
    private lateinit var playerSystem: EntitySystem<Player>
    private lateinit var attackSystem: EntitySystem<Attack>
    private lateinit var healthBarSystem: EntitySystem<HealthBar>
    private var player = Player()
    private lateinit var inventory: Inventory
    private lateinit var container: Container
    private lateinit var currentLevelText: Text

    // TODO: The depth range is aquired from the renderer
    // TODO: Create a method in scene to create a new camera which auto-injects the depth range
    private var camera = Camera(Vector2f(0.0f, 20.0f))
    private var level = Level()

    override fun init() {
        mobTexture = resourceFactory.loadTexture2d("./data/textures/dwarf.png", TextureFilter.NEAREST)
        mobTexture.setTiledTexture(16,16)
        mobMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", mobTexture, Vector3f(1.0f, 1.0f, 1.0f))
        player = Player()
        playerSystem = EntitySystem(scene)
        playerSystem.newEntity(player)
                .attachTransformComponent()
                .attachSpriteComponent(mobMaterial)
                .attachAnimatorComponent()
                .attachBoxColliderComponent(width = 24.0f, height = 32.0f)
                .build()
        scene.addSystem(playerSystem)

        val attackTexture = resourceFactory.loadTexture2d("./data/textures/attack.png", TextureFilter.NEAREST)
        attackTexture.setTiledTexture(16,16)
        attackMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", attackTexture, Vector3f(1.0f, 1.0f,
                1.0f))

        attackSystem = EntitySystem(scene)
        attackSystem.newEntity(player.attack)
                .attachTransformComponent()
                .attachSpriteComponent(attackMaterial)
                .attachAnimatorComponent()
                .attachBoxColliderComponent(32.0f, 32.0f)
                .build()
        scene.addSystem(attackSystem)

        val healthTexture = resourceFactory.loadTexture2d("./data/textures/health.png", TextureFilter.NEAREST)
        healthMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", healthTexture, Vector3f(1.0f, 1.0f,
                1.0f))
        healthBarSystem = EntitySystem(scene)
        scene.addSystem(healthBarSystem)

        // TODO: Constant window dimensions
        level.create(resourceFactory, scene, 8960 / 64, 5040 / 64, 1280 / 64, 720 / 64 + 1)
        level.build(resourceFactory, 0, healthBarSystem, healthMaterial)
        scene.addTilemap(level.backTilemap)
        scene.addTilemap(level.frontTilemap)
        scene.addTilemap(level.detailTilemap)
        scene.addTilemap(level.minimapTilemap)

        camera = Camera(Vector2f(0.0f, 20.0f))
        scene.setActiveCamera(camera)

        player.map = level.map
        player.width = level.width
        player.height = level.height
        player.mapWidth = level.mapWidth
        player.mapHeight = level.mapHeight
        player.maxCellX = level.maxCellX
        player.maxCellY = level.maxCellY
        player.tileWidth = 64
        player.setPosition(level.getFirstTilePos())
        level.switchCell(resourceFactory, player.cellX, player.cellY)

        inventory = Inventory(gui, player)
        player.inventory = inventory
        player.level = level

        // TODO: Constant window dimensions
        container = gui.newContainer(1280.0f/2.0f - 100, 720.0f - 40.0f, 200.0f, 40.0f)
        currentLevelText = container.addText("Current Level: ${player.currentLevel}", 0.0f, 0.0f, background = true)
        currentLevelText.x += currentLevelText.w/2.0f
    }

    override fun update() {
        if (player.health == 0) {
            // TODO: PLAYER DIED
        }

        level.update(player)

        if (player.playerMovedCell) {
            level.switchCell(resourceFactory, player.cellX, player.cellY)
            player.playerMovedCell = false
        }

        inventory.update()

        if (player.transform.x + player.cellX*level.width*64 >= level.exitPosition.x*64 - 32 && player.transform.x + player.cellX*level.width*64 <= level.exitPosition.x*64 + 32 &&
                player.transform.y + player.cellY*level.height*64 >= level.exitPosition.y*64 - 32 && player.transform.y + player.cellY*level.height*64 <= level.exitPosition.y*64 + 32) {
            level.build(resourceFactory, System.currentTimeMillis(), healthBarSystem, healthMaterial)
            player.setPosition(level.getFirstTilePos())
            level.switchCell(resourceFactory, player.cellX, player.cellY)

            player.currentLevel += 1
            container.removeText(currentLevelText)
            currentLevelText = container.addText("Current Level: ${player.currentLevel}", 0.0f, 0.0f, background = true)
            currentLevelText.x += currentLevelText.w/2.0f
        }
    }
}
