package rain.api.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.World
import org.joml.Vector2f
import org.joml.Vector3f
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.*

class Scene {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val entitySystems = ArrayList<EntitySystem<Entity>>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()
    lateinit var physicWorld: World
        private set
    private var physicsContactListener = PhysicsContactListener()

    private var camera = Camera(Vector2f(0.0f, 20.0f))
    private lateinit var emitterMaterial: Material

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
        this.emitterMaterial = resourceFactory.createMaterial("emitterMaterial", "./data/shaders/particle.vert.spv", "./data/shaders/particle.frag.spv",
                null, Vector3f(1.0f, 1.0f, 1.0f))

        Box2D.init()
        physicWorld = World(Vector2(0.0f, 0.0f), true)
        physicWorld.setContactListener(physicsContactListener)
    }

    internal fun update(renderer: Renderer, input: Input, deltaTime: Float) {
        physicWorld.step(1.0f / 60.0f, 6, 2)

        renderer.setActiveCamera(camera)

        val submitListSorted = ArrayList<Drawable>()
        var submitListParticles = ArrayList<Drawable>()
        for (tilemap in tilemaps) {
            submitListSorted.add(Drawable(tilemap.transform, tilemap.material, tilemap.getUniformData(), tilemap.vertexBuffer, null))
        }

        for (system in entitySystems) {
            for (entity in system.getEntityList()) {
                entity!!.update(this, input, system, deltaTime)
            }

            for (animator in system.getAnimatorList()) {
                if (!animator!!.animating) {
                    continue
                }

                animator.textureTileOffset.y = animator.animation.yPos
                if (animator.animationTime >= 1.0f) {
                    animator.animationTime = 0.0f
                    animator.animationIndex += 1

                    if (animator.animationIndex >= animator.animation.endFrame - animator.animation.startFrame) {
                        animator.animationIndex = 0
                    }

                    animator.textureTileOffset.x = animator.animation.startFrame + animator.animationIndex
                }

                animator.animationTime += deltaTime * animator.animation.speed
            }

            for (sprite in system.getSpriteList()) {
                if (!sprite!!.visible) {
                    continue
                }

                submitListSorted.add(Drawable(sprite.transform, sprite.material, sprite.getUniformData(), quadVertexBuffer, null))
            }

            for (emitter in system.getParticleEmitterList()) {
                emitter!!.update(system, deltaTime * 0.5f)
                submitListParticles.add(Drawable(emitter.transform, emitterMaterial, emitter.getUniformData(), emitter.vertexBuffer, emitter.indexBuffer))
            }

            for (collider in system.getColliderList()) {
                val b = collider!!.getBody()
                collider.transform.x = b.position.x
                collider.transform.y = b.position.y
            }
        }

        submitListSorted.sortBy { drawable -> drawable.transform.z }
        for (drawable in submitListSorted) {
            renderer.submitDraw(drawable)
        }
        for (drawable in submitListParticles) {
            renderer.submitDraw(drawable)
        }
    }

    fun clear() {
        entitySystems.clear()
        tilemaps.clear()
        cameras.clear()

        // The world needs to be alive - we just want to clear it from old colliders
        physicWorld.dispose()
    }
}
