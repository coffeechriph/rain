package rain.api.entity

import org.joml.Vector2f
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gfx.TextureFilter

internal lateinit var particleMaterial: Material
private lateinit var resourceFactory: ResourceFactory
private val particleEmitters = ArrayList<ParticleEmitter>()
private val burstParticleEmitters = ArrayList<BurstParticleEmitter>()

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

internal fun emitterManagerRemoveEmitter(emitter: ParticleEmitter) {
    emitter.destroy()
    particleEmitters.remove(emitter)
}

internal fun emitterManagerRemoveBurstEmitter(emitter: BurstParticleEmitter) {
    emitter.destroy()
    burstParticleEmitters.remove(emitter)
}

internal fun emitterManagerCreateBurstEmitter(transform: Transform, numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float, tickRate: Float = 1.0f): BurstParticleEmitter {
    val emitter = BurstParticleEmitter(particleMaterial, resourceFactory, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread)
    burstParticleEmitters.add(emitter)
    return emitter
}

internal fun emitterManagerCreateEmitter(transform: Transform, numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float, tickRate: Float = 1.0f): ParticleEmitter {
    val emitter = ParticleEmitter(particleMaterial, resourceFactory, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread, tickRate)
    particleEmitters.add(emitter)
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