package rain.api.entity

import rain.api.Input
import rain.api.gfx.renderManagerGetRenderComponentByEntity
import rain.api.scene.Scene

private var currentId: Long = 0
private fun getNextUniqueId(): Long {
    if (currentId + 1 >= Long.MAX_VALUE) {
        throw AssertionError("Creating this entity would cause the ID to overflow!")
    }
    currentId += 1

    return currentId
}

open class Entity {
    private val id = getNextUniqueId()

    fun getId(): Long {
        return id
    }

    fun getParticleEmitters(): List<ParticleEmitter>? {
        return emitterManagerGetEmitterFromId(id)
    }

    fun getBurstParticleEmitters(): List<BurstParticleEmitter>? {
        return emitterManagerGetBurstEmitterFromId(id)
    }

    fun getRenderComponents(): List<RenderComponent>? {
        return renderManagerGetRenderComponentByEntity(id)
    }

    fun getAnimatorComponent(): List<Animator>? {
        return animatorManagerGetAnimatorByEntity(id)
    }

    open fun<T: Entity> init(scene: Scene, system: EntitySystem<T>){}
    open fun<T: Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float){}
    open fun onCollision(entity: Entity) {}
}
