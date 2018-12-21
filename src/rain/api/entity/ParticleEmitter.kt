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
    var vertexBuffer: VertexBuffer
        private set
    var indexBuffer: IndexBuffer
        private set
    var startColor = Vector3f(1.0f, 0.0f, 0.0f)
    var endColor = Vector3f(0.0f, 1.0f, 0.0f)

    private var particles: FloatArray = FloatArray(numParticles*12)
    private var indices: IntArray = IntArray(numParticles*6)
    private var tick = 0.0f

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
            particles[index+10] = -0.5f
            particles[index+11] = i.toFloat() / factor
            index += 12
        }

        index = 0
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
        tick %= particleLifetime
    }
}
