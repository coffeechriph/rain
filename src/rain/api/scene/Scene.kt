package rain.api.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.World
import org.joml.Matrix4f
import org.joml.Random
import org.joml.Vector2f
import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.Window
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.ParticleEmitterEntity
import rain.api.entity.metersToPixels
import rain.api.gfx.*
import rain.api.manager.emitterManagerAddParticleEmitterEntity
import rain.vulkan.VertexAttribute

class Scene(val resourceFactory: ResourceFactory, val window: Window) {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val entitySystems = ArrayList<EntitySystem<Entity>>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()
    private val spriteBatchers = ArrayList<SpriteBatcher>()
    lateinit var physicWorld: World
        private set
    private var physicsContactListener = PhysicsContactListener()

    var activeCamera = Camera(1000.0f, Vector2i(window.size.x, window.size.y))
        set(value) {

        }

    fun<T: Entity> newSystem(material: Material?): EntitySystem<T> {
        //val texelBuffer = if (texture2d != null) { resourceFactory.createTexelBuffer(256) } else { null }
        lateinit var system: EntitySystem<T>
        if (material != null) {
            // TODO: We only have to copy the material if we use batching
            val materialCopy = material.copy()
            system = EntitySystem(this, materialCopy)
            entitySystems.add(system as EntitySystem<Entity>)

            if (material.useBatching()) {
                spriteBatchers.add(SpriteBatcher(system, materialCopy, resourceFactory))
            }
        }
        else {
            system = EntitySystem(this, null)
            entitySystems.add(system as EntitySystem<Entity>)
        }

        return system
    }

    fun addTilemap(tilemap: Tilemap) {
        tilemaps.add(tilemap)
    }

    fun addCamera(camera: Camera) {
        cameras.add(camera)
    }

    fun createParticleEmitter(particleLifetime: Float,
                           particleCount: Int,
                           particleSpread: Float): ParticleEmitterEntity {
        val emitter = ParticleEmitterEntity(
                resourceFactory,
                Random(System.currentTimeMillis()),
                particleLifetime,
                particleCount,
                particleSpread)
        emitterManagerAddParticleEmitterEntity(emitter)
        return emitter
    }

    // TODO: Remove sprite batchers as well when systems are deleted
    fun removeEntitySystem(system: EntitySystem<*>) {
        entitySystems.remove(system)
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
        // TODO: Optimize this
        val byteBuffer = memAlloc(24*4)
        byteBuffer.asFloatBuffer().put(vertices).flip()
        this.quadVertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(byteBuffer)
                .withState(VertexBufferState.STATIC)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 2))
                .build()

        Box2D.init()
        physicWorld = World(Vector2(0.0f, 0.0f), true)
        physicWorld.setContactListener(physicsContactListener)
    }

    internal fun update(input: Input, deltaTime: Float) {
        physicWorld.step(1.0f / 60.0f, 6, 2)
        physicWorld.clearForces()

        for (batcher in spriteBatchers) {
            //batcher.batch()
            //batcher.update()
        }

        for (system in entitySystems) {
            for (entity in system.getEntityList()) {
                entity!!.update(this, input, system, deltaTime)
            }

            for (collider in system.getColliderList()) {
                val b = collider!!.getBody()
                collider.transform.x = b.position.x * metersToPixels
                collider.transform.y = b.position.y * metersToPixels
            }
        }
    }

    internal fun render(renderer: Renderer) {
        renderer.setActiveCamera(activeCamera)

        /*for (batcher in spriteBatchers) {
            if (batcher.hasSprites()) {
                submitListSorted.add(Drawable(batcher.material, memAlloc(4), batcher.vertexBuffer, 0.0f))
            }
        }*/
    }

    fun clear() {
        for (tilemap in tilemaps) {
            tilemap.destroy()
        }
        tilemaps.clear()

        entitySystems.clear()
        cameras.clear()
        spriteBatchers.clear()
        physicWorld.dispose()
    }
}
