package rain.api

class Scene {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val entitySystems = ArrayList<EntitySystem<Entity>>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()

    private var camera = Camera()

    fun<T: Entity> addSystem(system: EntitySystem<T>) {
        entitySystems.add(system as EntitySystem<Entity>)
    }

    fun addTilemap(tilemap: Tilemap) {
        tilemaps.add(tilemap)
    }

    fun addCamera(camera: Camera) {
        cameras.add(camera)
    }

    fun setActiveCamera(camera: Camera) {
        this.camera = camera
    }

    internal fun init(resourceFactory: ResourceFactory) {
        val vertices = floatArrayOf(
                -0.5f, -0.5f, 0.0f, 0.0f,
                -0.5f, 0.5f, 0.0f, 1.0f,
                0.5f, 0.5f, 1.0f, 1.0f,

                0.5f, 0.5f, 1.0f, 1.0f,
                0.5f, -0.5f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.0f, 0.0f
        )
        this.quadVertexBuffer = resourceFactory.createVertexBuffer(vertices, VertexBufferState.STATIC)
    }

    internal fun update(renderer: Renderer, input: Input, deltaTime: Float) {
        renderer.setActiveCamera(camera)

        for (tilemap in tilemaps) {
            renderer.submitDraw(tilemap, tilemap.vertexBuffer)
        }

        for (system in entitySystems) {
            for (update in system.updateIterator()) {
                update.update(this, input, system, deltaTime)
            }

            for (sprite in system.spriteIterator()) {
                if (!sprite.visible) {
                    continue
                }

                sprite.animationTime += deltaTime * sprite.animation.speed
                if (sprite.animationTime >= 1.0f) {
                    sprite.animationTime = 0.0f
                    sprite.animationIndex += 1

                    sprite.textureTileOffset.x = sprite.animation.startFrame + sprite.animationIndex

                    if (sprite.animationIndex >= (sprite.animation.endFrame - sprite.animation.startFrame)) {
                        sprite.animationIndex = 0
                    }
                }
                val transform = system.findTransformComponent(sprite.entity)!!
                sprite.transform.position = transform.position
                sprite.transform.scale = transform.scale
                sprite.transform.rotation = transform.rotation
                renderer.submitDraw(sprite, quadVertexBuffer)
            }
        }
    }
}
