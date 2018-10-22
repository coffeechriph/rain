package rain.api

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*

class Scene {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val entitySystems = ArrayList<EntitySystem<Entity>>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()
    lateinit var physicWorld: World
        private set
    private var physicsContactListener = PhysicsContactListener()

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

        Box2D.init()
        physicWorld = World(Vector2(0.0f, 0.0f), true)
        physicWorld.setContactListener(physicsContactListener)
    }

    internal fun update(renderer: Renderer, input: Input, deltaTime: Float) {
        physicWorld.step(1.0f / 60.0f, 6, 2)

        renderer.setActiveCamera(camera)

        for (tilemap in tilemaps) {
            renderer.submitDraw(tilemap, tilemap.vertexBuffer)
        }

        for (system in entitySystems) {
            for (i in 0 until system.getEntityList().size) {
                system.getEntityList()[i]!!.update(this, input, system, deltaTime)
            }

            for (i in 0 until system.getSpriteList().size) {
                val sprite = system.getSpriteList().get(i)!!
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

            for (collider in system.getColliderList()) {
                val b = collider!!.getBody()
                val e = b.userData as Entity
                val t = system.findTransformComponent(e.getId())!!
                t.x = b.position.x
                t.y = b.position.y
            }
        }
    }
}
