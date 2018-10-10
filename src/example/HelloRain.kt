package example

import org.joml.Vector3f
import rain.Api
import rain.Rain
import rain.api.*
import rain.api.Tilemap.TileIndex

class Player : Entity() {
    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        val sprite = system.findSpriteComponent(id)!!
        sprite.addAnimation("walk", 0, 4, 0)
        println("Initialize Entity!")
    }

    override fun<T: Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        val transformComponent = system.findTransformComponent(id)!!

        transformComponent.transform.position.set(512.0f, 256.0f)
        transformComponent.transform.scale.set(64.0f, 64.0f)

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
    lateinit var tilemapMaterial: Material
    lateinit var tilemapTexture: Texture2d
    lateinit var playerEntitySystem: EntitySystem<Player>
    lateinit var player: Player
    lateinit var camera: Camera
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
        playerEntitySystem.newEntity(player)
                .attachUpdateComponent()
                .attachTransformComponent()
                .attachSpriteComponent(basicMaterial)
                .build(scene)

        scene.addSystem(playerEntitySystem)

        for (i in 0 until 16) {
            mapIndices[i] = TileIndex(1, 0)
            mapIndices[i * 16] = TileIndex(2, 0)
        }

        tilemap.create(resourceFactory, tilemapMaterial, 16, 16, 32.0f, 32.0f, mapIndices)
        tilemap.getTransform().position.set(0.0f, 0.0f)
        tilemap.getTransform().scale.set(1.0f, 1.0f)
        scene.addTilemap(tilemap)

        camera = Camera()
        scene.setActiveCamera(camera)
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
