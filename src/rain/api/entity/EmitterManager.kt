package rain.api.entity

import org.joml.Vector2f
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gfx.TextureFilter

private lateinit var particleMaterial: Material
private lateinit var resourceFactory: ResourceFactory
private val particleEmitters = ArrayList<ParticleEmitter>()
private val burstParticleEmitters = ArrayList<BurstParticleEmitter>()

private val particleEmittersMap = HashMap<Long, ArrayList<ParticleEmitter>>()
private val burstParticleEmittersMap = HashMap<Long, ArrayList<BurstParticleEmitter>>()

internal fun emitterManagerInit(factory: ResourceFactory) {
    resourceFactory = factory
    val fireTexture = resourceFactory.buildTexture2d()
            .withName("fireTexture")
            .fromImageFile("./data/textures/fire.png")
            .withFilter(TextureFilter.NEAREST)
            .build()

    // TODO: This shouldn't be part of the engine either
    particleMaterial = resourceFactory.buildMaterial()
            .withName("emitterMaterial")
            .withVertexShader("./data/shaders/particle.vert.spv")
            .withFragmentShader("./data/shaders/particle.frag.spv")
            .withTexture(fireTexture)
            .build()
}

internal fun emitterManagerGetEmitterFromId(entityId: Long): List<ParticleEmitter>? {
    return particleEmittersMap[entityId]
}

internal fun emitterManagerGetBurstEmitterFromId(entityId: Long): List<BurstParticleEmitter>? {
    return burstParticleEmittersMap[entityId]
}

internal fun emitterManagerRemoveEmitterByEntity(entityId: Long) {
    val emitters = particleEmittersMap.remove(entityId)
    if (emitters != null) {
        for (emitter in emitters) {
            emitter.destroy()
            particleEmitters.remove(emitter)
        }
    }
}

internal fun emitterManagerRemoveBurstEmitterByEntity(entityId: Long) {
    val emitters = burstParticleEmittersMap.remove(entityId)
    if (emitters != null) {
        for (emitter in emitters) {
            emitter.destroy()
            burstParticleEmitters.remove(emitter)
        }
    }
}

internal fun emitterManagerRemoveEmitter(emitter: ParticleEmitter) {
    emitter.destroy()
    particleEmitters.remove(emitter)
}

internal fun emitterManagerRemoveBurstEmitter(emitter: BurstParticleEmitter) {
    emitter.destroy()
    burstParticleEmitters.remove(emitter)
}

internal fun emitterManagerCreateBurstEmitter(entityId: Long, transform: Transform, numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float, tickRate: Float = 1.0f): BurstParticleEmitter {
    val emitter = BurstParticleEmitter(particleMaterial, resourceFactory, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread)
    burstParticleEmitters.add(emitter)

    if (burstParticleEmittersMap.containsKey(entityId)) {
        burstParticleEmittersMap[entityId]!!.add(emitter)
    }
    else {
        val list = ArrayList<BurstParticleEmitter>()
        list.add(emitter)
        burstParticleEmittersMap[entityId] = list
    }
    return emitter
}

internal fun emitterManagerCreateEmitter(entityId: Long, transform: Transform, numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float, tickRate: Float = 1.0f): ParticleEmitter {
    val emitter = ParticleEmitter(entityId, particleMaterial, resourceFactory, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread, tickRate)
    particleEmitters.add(emitter)

    if (particleEmittersMap.containsKey(entityId)) {
        particleEmittersMap[entityId]!!.add(emitter)
    }
    else {
        val list = ArrayList<ParticleEmitter>()
        list.add(emitter)
        particleEmittersMap[entityId] = list
    }
    return emitter
}

internal fun emitterManagerSimulate() {
    for (emitter in particleEmitters) {
        if (!emitter.enabled) {
            continue
        }

        emitter.update()
    }

    for (emitter in burstParticleEmitters) {
        if (!emitter.enabled) {
            continue
        }

        emitter.update()
    }
}