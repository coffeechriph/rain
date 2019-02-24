package rain.api.manager

import rain.api.components.RenderComponent
import rain.api.entity.ParticleEmitterEntity
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gfx.TextureFilter

private lateinit var particleMaterial: Material
private lateinit var resourceFactory: ResourceFactory

private val particleEmitterEntities = ArrayList<ParticleEmitterEntity>()

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

internal fun emitterManagerClear() {
    for (emitter in particleEmitterEntities) {
        renderManagerRemoveRenderComponentByEntity(emitter.getId())
    }
    particleEmitterEntities.clear()
}

internal fun emitterManagerAddParticleEmitterEntity(emitter: ParticleEmitterEntity) {
    val renderComponent = RenderComponent(emitter.transform, emitter.mesh, particleMaterial)
    renderComponent.createUniformData = emitter::getUniformData
    renderManagerAddRenderComponent(emitter.getId(), renderComponent)
    particleEmitterEntities.add(emitter)
}

internal fun emitterManagerRemoveParticleEmitterEntity(emitter: ParticleEmitterEntity) {
    renderManagerRemoveRenderComponentByEntity(emitter.getId())
    particleEmitterEntities.remove(emitter)
}

internal fun emitterManagerSimulate() {
    for (emitter in particleEmitterEntities) {
        emitter.simulate()
    }
}
