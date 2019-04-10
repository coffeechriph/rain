package rain.api.scene

import org.joml.Random
import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.ParticleEmitterEntity
import rain.api.gfx.Material
import rain.api.gfx.Renderer
import rain.api.gfx.ResourceFactory
import rain.api.manager.animatorManagerRemoveAnimatorByEntity
import rain.api.manager.emitterManagerAddParticleEmitterEntity
import rain.api.manager.renderManagerRemoveRenderComponentByEntity

abstract class Scene internal constructor(val sceneManager: SceneManager, val resourceFactory: ResourceFactory) {
    private val entities = ArrayList<Entity>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()
    var activeCamera = Camera(1000.0f, Vector2i(1280, 720))

    open fun init(){}
    open fun update(input: Input){}

    internal fun doUpdate(input: Input, renderer: Renderer) {
        update(input)
        for (entity in entities) {
            entity.update(this, input)
        }
        for (tilemap in tilemaps) {
            tilemap.updateRenderComponent()
        }
        renderer.setActiveCamera(activeCamera)
    }

    fun<T: Entity> newEntity(entity: T): EntityBuilder<T> {
        entities.add(entity)
        return EntityBuilder(this, entity)
    }

    fun removeEntity(entity: Entity) {
        entities.remove(entity)
        renderManagerRemoveRenderComponentByEntity(entity.getId())
        animatorManagerRemoveAnimatorByEntity(entity.getId())
    }

    fun createTilemap(material: Material, tileNumX: Int, tileNumY: Int, tileWidth: Float, tileHeight: Float): Tilemap {
        val tilemap = Tilemap()
        tilemaps.add(tilemap)
        tilemap.create(resourceFactory, material, tileNumX, tileNumY, tileWidth, tileHeight)
        return tilemap
    }

    fun removeTilemap(tilemap: Tilemap) {
        tilemaps.remove(tilemap)
        tilemap.destroy()
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

    fun clear() {
        for (tilemap in tilemaps) {
            tilemap.destroy()
        }
        tilemaps.clear()

        for (entity in entities) {
            renderManagerRemoveRenderComponentByEntity(entity.getId())
        }
        entities.clear()
        cameras.clear()
    }
}
