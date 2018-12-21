package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import rain.api.gfx.IndexBuffer
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.util.*

class ParticleEmitter constructor(resourceFactory: ResourceFactory, val transform: Transform, private val numParticles: Int, private val particleSize: Float, private val particleLifetime: Float, private val particleVelocity: Vector2f, private val directionType: DirectionType, particleSpread: Float) {
    var vertexBuffer: VertexBuffer
        private set
    var indexBuffer: IndexBuffer
        private set
    var startColor = Vector4f(1.0f, 0.0f, 0.0f, 1.0f)
    var endColor = Vector4f(0.0f, 1.0f, 0.0f, 0.0f)

    private var particles: FloatArray = FloatArray(numParticles*12)
    private var indices: IntArray = IntArray(numParticles*6)
    private var offsets: FloatArray = FloatArray(numParticles)
    private var tick = 0.0f

    private val modelMatrixBuffer = MemoryUtil.memAlloc(24 * 4)
    private val modelMatrix = Matrix4f()

    init {
        val random = Random()

        if (directionType == DirectionType.LINEAR) {
            for (i in 0 until numParticles) {
                offsets[i] = ((random.nextFloat() - 0.5f) * particleSpread)
            }
        }
        else {
            for (i in 0 until numParticles) {
                offsets[i] = (random.nextDouble()*Math.PI*2).toFloat()
            }
        }

        var index = 0
        var vi = 0
        for (i in 0 until numParticles) {
            indices[index] = vi
            indices[index+1] = vi+1
            indices[index+2] = vi+2

            indices[index+3] = vi+2
            indices[index+4] = vi+3
            indices[index+5] = vi
            index += 6
            vi += 4
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
        fbuf.put(16, startColor.x)
        fbuf.put(17, startColor.y)
        fbuf.put(18, startColor.z)
        fbuf.put(19, startColor.w)
        fbuf.put(20, endColor.x)
        fbuf.put(21, endColor.y)
        fbuf.put(22, endColor.z)
        fbuf.put(23, endColor.w)
        return modelMatrixBuffer
    }

    fun update(entitySystem: EntitySystem<Entity>, deltaTime: Float) {
        tick += deltaTime

        val psize = particleSize * 0.5f
        val factor = particleLifetime / numParticles

        if (directionType == DirectionType.LINEAR) {
            updateParticlesLinear(factor, psize)
        }
        else {
            updateParticlesCircular(factor, psize)
        }

        vertexBuffer.update(particles)
    }

    private fun updateParticlesLinear(factor: Float, psize: Float) {
        var index1 = 0
        val vx = particleVelocity.x
        val vy = particleVelocity.y

        for (i in 0 until numParticles) {
            val k = (((i.toFloat() * factor) + tick) % particleLifetime) / particleLifetime
            val x1 = -psize * k + vx * k + offsets[i]
            val x2 = psize * k + vx * k + offsets[i]
            val y1 = -psize * k + vy * k
            val y2 = psize * k + vy * k

            particles[index1] = x1
            particles[index1 + 1] = y1
            particles[index1 + 2] = k

            particles[index1 + 3] = x1
            particles[index1 + 4] = y2
            particles[index1 + 5] = k

            particles[index1 + 6] = x2
            particles[index1 + 7] = y2
            particles[index1 + 8] = k

            particles[index1 + 9] = x2
            particles[index1 + 10] = y1
            particles[index1 + 11] = k
            index1 += 12
        }
    }

    private fun updateParticlesCircular(factor: Float, psize: Float) {
        var index1 = 0
        for (i in 0 until numParticles) {
            val vx = (Math.sin(offsets[i].toDouble()) * particleVelocity.x).toFloat()
            val vy = (Math.cos(offsets[i].toDouble()) * particleVelocity.y).toFloat()

            val k = ((((i.toFloat() * factor) + tick) % particleLifetime) / particleLifetime)
            val x1 = -psize * k + vx * k
            val x2 = psize * k + vx * k
            val y1 = -psize * k + vy * k
            val y2 = psize * k + vy * k

            particles[index1] = x1
            particles[index1 + 1] = y1
            particles[index1 + 2] = k

            particles[index1 + 3] = x1
            particles[index1 + 4] = y2
            particles[index1 + 5] = k

            particles[index1 + 6] = x2
            particles[index1 + 7] = y2
            particles[index1 + 8] = k

            particles[index1 + 9] = x2
            particles[index1 + 10] = y1
            particles[index1 + 11] = k
            index1 += 12
        }
    }
}
