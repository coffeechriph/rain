package rain.api.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.World
import org.joml.Matrix4f
import org.joml.Vector2f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.metersToPixels
import rain.api.gfx.*
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute

class Scene(val resourceFactory: ResourceFactory) {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val entitySystems = ArrayList<EntitySystem<Entity>>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()
    private val simpleDraws = ArrayList<SimpleDraw>()
    private val spriteBatchers = ArrayList<SpriteBatcher>()
    lateinit var physicWorld: World
        private set
    private var physicsContactListener = PhysicsContactListener()

    var activeCamera = Camera(Vector2f(0.0f, 20.0f))
    private lateinit var emitterMaterial: Material

    fun<T: Entity> newSystem(material: Material?): EntitySystem<T> {
        //val texelBuffer = if (texture2d != null) { resourceFactory.createTexelBuffer(256) } else { null }
        lateinit var system: EntitySystem<T>
        if (material != null) {
            // TODO: We only have to copy the material if we use batching
            val materialCopy = material.copy()
            system = EntitySystem<T>(this, materialCopy)
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

    fun addSimpleDraw(simpleDraw: SimpleDraw) {
        simpleDraws.add(simpleDraw)
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
        this.quadVertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(vertices)
                .withState(VertexBufferState.STATIC)
                .withAttribute(VertexAttribute(0, 2, DataType.FLOAT))
                .withAttribute(VertexAttribute(1, 2, DataType.FLOAT))
                .build()

        val fireTexture = resourceFactory.loadTexture2d("fireTexture", "./data/textures/fire.png", TextureFilter.NEAREST)
        this.emitterMaterial = resourceFactory.buildMaterial()
                .withName("emitterMaterial")
                .withVertexShader("./data/shaders/particle.vert.spv")
                .withFragmentShader("./data/shaders/particle.frag.spv")
                .withTexture(fireTexture)
                .build()

        Box2D.init()
        physicWorld = World(Vector2(0.0f, 0.0f), true)
        physicWorld.setContactListener(physicsContactListener)
    }

    internal fun update(input: Input, deltaTime: Float) {
        physicWorld.step(1.0f / 60.0f, 6, 2)
        physicWorld.clearForces()

        for (batcher in spriteBatchers) {
            batcher.batch()
            batcher.update()
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

            for (emitter in system.getParticleEmitterList()) {
                if (!emitter!!.enabled) {
                    continue
                }

                emitter.update()
            }

            for (emitter in system.getBurstParticleEmitterList()) {
                if (!emitter!!.enabled || !emitter.simulating) {
                    continue
                }

                emitter.update()
            }

            for (collider in system.getColliderList()) {
                val b = collider!!.getBody()
                collider.transform.x = b.position.x * metersToPixels
                collider.transform.y = b.position.y * metersToPixels
            }
        }
    }

    internal fun render(renderer: Renderer, resourceFactory: ResourceFactory) {
        renderer.setActiveCamera(activeCamera)
        val submitListSorted = ArrayList<Drawable>()
        val submitListParticles = ArrayList<Drawable>()

        for (tilemap in tilemaps) {
            submitListSorted.add(Drawable(tilemap.material, tilemap.getUniformData(), tilemap.vertexBuffer, tilemap.transform.z))
        }

        for (simpleDraw in simpleDraws) {
            val modelMatrix = Matrix4f()
            modelMatrix.identity()
            modelMatrix.rotateZ(simpleDraw.transform.rot)
            modelMatrix.translate(simpleDraw.transform.x, simpleDraw.transform.y, simpleDraw.transform.z)
            modelMatrix.scale(simpleDraw.transform.sx, simpleDraw.transform.sy, 0.0f)

            val byteBuffer = MemoryUtil.memAlloc(16 * 4)
            modelMatrix.get(byteBuffer) ?: throw IllegalStateException("Unable to get matrix content!")

            submitListSorted.add(Drawable(simpleDraw.material, byteBuffer, simpleDraw.vertexBuffer, simpleDraw.transform.z))
        }

        for (batcher in spriteBatchers) {
            if (batcher.hasSprites()) {
                submitListSorted.add(Drawable(batcher.material, memAlloc(4), batcher.vertexBuffer, 0.0f))
            }
        }

        for (system in entitySystems) {
            if (system.material != null && !system.material.useBatching()) {
                for (sprite in system.getSpriteList()) {
                    if (!sprite!!.visible) {
                        continue
                    }

                    submitListSorted.add(Drawable(system.material, sprite.getUniformData(), quadVertexBuffer, sprite.transform.z))
                }
            }

            for (emitter in system.getParticleEmitterList()) {
                if (!emitter!!.enabled) {
                    continue
                }

                submitListParticles.add(Drawable(emitterMaterial, emitter.getUniformData(), emitter.vertexBuffer, emitter.parentTransform.z + emitter.transform.z, emitter.indexBuffer))
            }

            for (emitter in system.getBurstParticleEmitterList()) {
                if (!emitter!!.enabled || !emitter.simulating) {
                    continue
                }

                if (!emitter.burstFinished) {
                    submitListParticles.add(Drawable(emitterMaterial, emitter.getUniformData(), emitter.vertexBuffer, emitter.parentTransform.z + emitter.transform.z, emitter.indexBuffer))
                }
            }
        }

        // TODO: Right now we must draw stuff in correct order as we're using alpha blending per default..
        submitListSorted.sortBy { drawable -> drawable.z }
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
        simpleDraws.clear()
        spriteBatchers.clear()
        physicWorld.dispose()
    }
}
