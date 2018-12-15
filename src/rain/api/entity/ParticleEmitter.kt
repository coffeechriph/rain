package rain.api.entity

import org.joml.Vector2f

class ParticleEmitter constructor(internal val entityId: Long, numParticles: Int, val particleSize: Float, particleLifetime: Float, val particleVelocity: Vector2f) {
    private var particles: FloatArray
    private var tick = 0.0f
    private var particleSizeFactor: Float

    init {
        this.particleSizeFactor = particleSize / particleLifetime

        particles = FloatArray(numParticles*3)
        var index = 0
        for (i in 0 until particles.size/3) {
            particles[index] = particleVelocity.x * i
            particles[index+1] = particleVelocity.y * i
            particles[index+2] = particleSizeFactor * i
            index += 3
        }
    }

    fun update(entitySystem: EntitySystem<Entity>, deltaTime: Float) {
        val transform = entitySystem.findTransformComponent(entityId)!!
        tick += deltaTime
        tick %= 1.0f

        var index = 0
        for (i in 0 until particles.size/3) {
            particles[index] = transform.x + particleVelocity.x * i + tick * particleVelocity.x
            particles[index+1] = transform.y + particleVelocity.y * i + tick * particleVelocity.y
            particles[index+2] = particleSizeFactor * i
            index += 3
        }
    }
}
