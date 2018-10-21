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
            for (i in 0 until system.getEntityList().size) {
                system.getEntityList()[i].update(this, input, system, deltaTime)
            }

            // TODO: Performance!
            // We're currently checking every collider in every system every other collider in every system...
            // We could flatten the list of colliders but for this we need a way to identify which system a entityId is part of
            // This could be done by storing the index of the system in the first 16 bits of the entityId
            // This means we can have 65535 different entity systems and way enough entities per system
            // The next thing to do is to divide the colliders up into quad trees so we never have to even think about colliders
            // that are too far away
            for (i in 0 until system.getColliderList().size) {
                val collider1 = system.getColliderList()[i]
                if (!collider1.active) {
                    continue
                }

                val transform1 = system.findTransformComponent(collider1.entityId)!!
                collider1.x = transform1.x
                collider1.y = transform1.y

                for (system2 in entitySystems) {
                    for (j in 0 until system2.getColliderList().size) {
                        val collider2 = system2.getColliderList()[j]
                        if (collider1 != collider2 && !collider2.active) {
                            continue
                        }

                        val transform2 = system2.findTransformComponent(collider2.entityId)!!
                        collider2.x = transform2.x
                        collider2.y = transform2.y
                        if (collider1.collides(collider2)) {
                            system.findEntity(collider1.entityId)!!.onCollision(collider2)
                        }
                    }
                }
            }

            for (i in 0 until system.getSpriteList().size) {
                val sprite = system.getSpriteList().get(i)
                if (!sprite.visible) {
                    continue
                }

                if (sprite.animationTime >= 1.0f) {
                    sprite.animationTime = 0.0f
                    sprite.animationIndex += 1

                    sprite.textureTileOffset.x = sprite.animation.startFrame + sprite.animationIndex

                    if (sprite.animationIndex >= (sprite.animation.endFrame - sprite.animation.startFrame)) {
                        sprite.animationIndex = 0
                    }
                }
                sprite.animationTime += deltaTime * sprite.animation.speed
                renderer.submitDraw(sprite, quadVertexBuffer)
            }
        }
    }
}
