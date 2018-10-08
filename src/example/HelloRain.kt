package example

import org.joml.Vector2f
import org.joml.Vector3f
import rain.Api
import rain.Rain
import rain.api.*
import rain.api.Tilemap.TileIndex

class Player {
    var x = 0.0f
    var tileIndexX = 0
    var tileIndexY = 0
    var counter = 0

    fun init(id: Long, system: EntitySystem, scene: Scene) {
        println("Initializing the player")
    }

    fun update(scene: Scene, input: Input, transformComponent: TransformComponent, spriteComponent: SpriteComponent) {
        transformComponent.transform.position.set(x, 0.0f)
        transformComponent.transform.scale.set(0.1f, 0.16f)

        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
            println("Left was pressed!")
        }
        else if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.RELEASED) {
            println("Left was released!")
        }

        counter++
        if (counter >= 20) {
            tileIndexX += 1
            if (tileIndexX >= 4) {
                tileIndexX = 0
                tileIndexY += 1
                if (tileIndexY >= 1) {
                    tileIndexY = 0
                    tileIndexX = 0
                }
            }
            counter = 0
        }
        spriteComponent.textureTileOffset.set(tileIndexX, tileIndexY)
    }
}

class HelloRain: Rain() {
    lateinit var basicMaterial: Material
    lateinit var basicTexture: Texture2d
    lateinit var tilemapMaterial: Material
    lateinit var tilemapTexture: Texture2d
    lateinit var playerEntitySystem: EntitySystem
    lateinit var player: Player
    val mapIndices = Array(16*16){ TileIndex(0,0) }
    val tilemap = Tilemap()

    override fun init() {
        basicTexture = resourceFactory.createTexture2d("./data/textures/sprite.png", TextureFilter.NEAREST)
        basicTexture.setTiledTexture(32, 32)
        tilemapTexture = resourceFactory.createTexture2d("./data/textures/tiles.png", TextureFilter.NEAREST)
        tilemapTexture.setTiledTexture(16,16)

        basicMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", basicTexture, Vector3f(1.0f,1.0f,1.0f))
        tilemapMaterial = resourceFactory.createMaterial("./data/shaders/tilemap.vert.spv", "./data/shaders/basic.frag.spv", tilemapTexture, Vector3f(1.0f,1.0f,1.0f))

        playerEntitySystem = EntitySystem()
        player = Player()
        playerEntitySystem.newEntity()
                .attachUpdateComponent(player::update)
                .attachTransformComponent()
                .attachSpriteComponent(basicMaterial)
                .build(scene, player::init)
        scene.registerSystem(playerEntitySystem)

        for (i in 0 until 16) {
            mapIndices[i] = TileIndex(1, 0)
            mapIndices[i * 16] = TileIndex(2, 0)
        }
        tilemap.create(resourceFactory, tilemapMaterial, 16, 16, 0.1f, 0.166f, mapIndices)
        tilemap.transform.position.set(-0.7f, -0.6f)
        scene.registerTilemap(tilemap)
    }

    override fun update() {
        if (input.keyState(Input.Key.KEY_0) == Input.InputState.PRESSED) {
            for (i in 0 until 16) {
                mapIndices[i] = TileIndex(0, 0)
                mapIndices[i * 16] = TileIndex(1, 0)
            }
            tilemap.update(mapIndices)
        }
    }
}

fun main(args: Array<String>) {
    val app = HelloRain()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
