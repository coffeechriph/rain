package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2f
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.util.*

class ParticleEmitter constructor(resourceFactory: ResourceFactory, val transform: Transform, numParticles: Int, private val particleSize: Float, private val particleLifetime: Float, private val particleVelocity: Vector2f, particleSpread: Float) {
    private var particles: FloatArray = FloatArray(numParticles*18)
    private var tick = 0.0f

    var vertexBuffer: VertexBuffer
        private set

    private val modelMatrixBuffer = MemoryUtil.memAlloc(21 * 4)
    private val modelMatrix = Matrix4f()

    init {
        var index = 0
        val factor = numParticles / particleLifetime
        val spread = particleSpread / particleSize
        val random = Random()

        for (i in 0 until numParticles) {
            val ox = (random.nextFloat() - 0.5f) * spread
            particles[index] = -0.5f + ox
            particles[index+1] = -0.5f
            particles[index+2] = i.toFloat() / factor

            particles[index+3] = -0.5f + ox
            particles[index+4] = 0.5f
            particles[index+5] = i.toFloat() / factor

            particles[index+6] = 0.5f + ox
            particles[index+7] = 0.5f
            particles[index+8] = i.toFloat() / factor

            particles[index+9] = 0.5f + ox
            particles[index+10] = 0.5f
            particles[index+11] = i.toFloat() / factor

            particles[index+12] = 0.5f + ox
            particles[index+13] = -0.5f
            particles[index+14] = i.toFloat() / factor

            particles[index+15] = -0.5f + ox
            particles[index+16] = -0.5f
            particles[index+17] = i.toFloat() / factor
            index += 18
        }

        vertexBuffer = resourceFactory.createVertexBuffer(particles, VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 3)))
    }

    fun getUniformData(): ByteBuffer {
        modelMatrix.identity()
        modelMatrix.rotateZ(transform.rot)
        modelMatrix.translate(transform.x, transform.y, transform.z)

        val buffer = modelMatrix.get(modelMatrixBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
        val fbuf = buffer.asFloatBuffer()
        fbuf.put(16, particleVelocity.x)
        fbuf.put(17, particleVelocity.y)
        fbuf.put(18, tick)
        fbuf.put(19, particleLifetime)
        fbuf.put(20, particleSize)
        return modelMatrixBuffer
    }

    fun update(entitySystem: EntitySystem<Entity>, deltaTime: Float) {
        tick += deltaTime
        tick %= particleLifetime
    }
}
