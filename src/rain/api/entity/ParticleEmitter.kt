package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import rain.api.gfx.IndexBuffer
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.util.*

class ParticleEmitter constructor(resourceFactory: ResourceFactory, val transform: Transform, numParticles: Int, private val particleSize: Float, private val particleLifetime: Float, private val particleVelocity: Vector2f, particleSpread: Float) {
    data class Particle (val index: Int, var y: Float)
    var vertexBuffer: VertexBuffer
        private set
    var indexBuffer: IndexBuffer
        private set
    var startColor = Vector3f(1.0f, 0.0f, 0.0f)
    var endColor = Vector3f(0.0f, 1.0f, 0.0f)

    private var particles: FloatArray = FloatArray(numParticles*18)
    private var indices: IntArray = IntArray(numParticles*6)
    private var particleY = Array<Particle>(numParticles){i -> Particle (0,0.0f) }
    private var tick = 0.0f
    private var itick = 0

    private val modelMatrixBuffer = MemoryUtil.memAlloc(29 * 4)
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
            particleY[i] = Particle(i, 0.0f)
            index += 18
        }

        index = 0
        for (i in 0 until numParticles) {
            indices[index] = index
            indices[index+1] = index+1
            indices[index+2] = index+2

            indices[index+3] = index+3
            indices[index+4] = index+4
            indices[index+5] = index+5
            index += 6
        }

        vertexBuffer = resourceFactory.createVertexBuffer(particles, VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 3)))
        indexBuffer = resourceFactory.createIndexBuffer(indices, VertexBufferState.DYNAMIC)
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
        fbuf.put(20, startColor.x)
        fbuf.put(21, startColor.y)
        fbuf.put(22, startColor.z)
        fbuf.put(23, 0.0f)
        fbuf.put(24, endColor.x)
        fbuf.put(25, endColor.y)
        fbuf.put(26, endColor.z)
        fbuf.put(27, 1.0f)
        fbuf.put(28, particleSize)
        return modelMatrixBuffer
    }

    fun update(entitySystem: EntitySystem<Entity>, deltaTime: Float) {
        tick += deltaTime
        itick = tick.toInt()
        tick %= particleLifetime

        var findex = 0
        for (i in 0 until particleY.size) {
            val ftime = (particles[findex+2]+tick) % particleLifetime
            particleY[i].y = (particleLifetime - ftime) / particleLifetime
            findex += 18
        }

        particleY.sortByDescending { particle -> particle.y }

        var index = 0
        for (i in 0 until particleY.size) {
            indices[index] = particleY[i].index*6
            indices[index+1] = particleY[i].index*6+1
            indices[index+2] = particleY[i].index*6+2

            indices[index+3] = particleY[i].index*6+3
            indices[index+4] = particleY[i].index*6+4
            indices[index+5] = particleY[i].index*6+5
            index += 6
        }

        indexBuffer.update(indices)
    }
}
