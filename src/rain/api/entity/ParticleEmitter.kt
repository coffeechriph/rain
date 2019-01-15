package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import rain.api.gfx.IndexBuffer
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.util.*

class ParticleEmitter constructor(private val resourceFactory: ResourceFactory, val parentTransform: Transform, private val numParticles: Int, private val
particleSize: Float, private val particleLifetime: Float, private val particleVelocity: Vector2f, private val directionType: DirectionType, private val particleSpread: Float, private val tickRate: Float = 1.0f) {
    data class Particle (var x: Float, var y: Float, var i: Float)

    var vertexBuffer: VertexBuffer
        private set
    var indexBuffer: IndexBuffer
        private set
    var transform = Transform()
    var startColor = Vector4f(1.0f, 0.0f, 0.0f, 1.0f)
    var endColor = Vector4f(0.0f, 1.0f, 0.0f, 1.0f)
    var startSize = 1.0f
    var enabled = true

    private var particles: Array<Particle> = Array(numParticles){ Particle(0.0f, 0.0f, 0.0f) }
    private var bufferData: FloatArray = FloatArray(numParticles*20)
    private var indices: IntArray = IntArray(numParticles*6)
    private var offsets: FloatArray
    private var tick = 0.0f

    private val modelMatrixBuffer = MemoryUtil.memAlloc(24 * 4)
    private val modelMatrix = Matrix4f()

    init {
        val random = Random()

        if (directionType == DirectionType.LINEAR) {
            offsets = FloatArray(numParticles*2)
            for (i in 0 until numParticles) {
                offsets[i*2] = ((random.nextFloat() - 0.5f) * particleSpread)
                offsets[i*2+1] = ((random.nextFloat() - 0.5f) * particleSpread)
            }
        }
        else {
            offsets = FloatArray(numParticles*2)
            for (i in 0 until numParticles) {
                offsets[i*2] = (random.nextDouble()*Math.PI*2).toFloat()
                offsets[i*2+1] = random.nextFloat()
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

        vertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(bufferData)
                .withState(VertexBufferState.DYNAMIC)
                .withAttribute(VertexAttribute(0, 3))
                .withAttribute(VertexAttribute(1, 2))
                .build()

        indexBuffer = resourceFactory.createIndexBuffer(indices, VertexBufferState.DYNAMIC)
    }

    fun getUniformData(): ByteBuffer {
        modelMatrix.identity()
        modelMatrix.rotateZ(parentTransform.rot)
        modelMatrix.translate(parentTransform.x + transform.x, parentTransform.y + transform.y, parentTransform.z + transform.z)

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

    fun clear() {
        resourceFactory.deleteVertexBuffer(vertexBuffer)
        resourceFactory.deleteIndexBuffer(indexBuffer)
    }

    fun update() {
        tick += 1.0f / 60.0f * tickRate

        val psize = particleSize * 0.5f
        val factor = particleLifetime / numParticles

        if (directionType == DirectionType.LINEAR) {
            updateParticlesLinear(factor, psize)
        } else {
            updateParticlesCircular(factor, psize)
        }

        vertexBuffer.update(bufferData)
    }

    private fun updateParticlesLinear(factor: Float, psize: Float) {
        var index1 = 0
        val vx = particleVelocity.x * particleLifetime
        val vy = particleVelocity.y * particleLifetime

        for (i in 0 until numParticles) {
            val k = (((i.toFloat() * factor) + tick) % particleLifetime)
            particles[i].x = vx * k + offsets[i*2]
            particles[i].y = vy * k + offsets[i*2+1]
            particles[i].i = k / particleLifetime
        }

        particles.sortByDescending { p -> p.i }

        for (i in 0 until numParticles) {
            val k = particles[i].i

            bufferData[index1] = particles[i].x - psize*k - startSize*0.5f
            bufferData[index1 + 1] = particles[i].y - psize*k - startSize*0.5f
            bufferData[index1 + 2] = k
            bufferData[index1 + 3] = 0.0f
            bufferData[index1 + 4] = 0.0f

            bufferData[index1 + 5] = particles[i].x - psize*k - startSize*0.5f
            bufferData[index1 + 6] = particles[i].y + psize*k + startSize*0.5f
            bufferData[index1 + 7] = k
            bufferData[index1 + 8] = 0.0f
            bufferData[index1 + 9] = 1.0f

            bufferData[index1 + 10] = particles[i].x + psize*k + startSize*0.5f
            bufferData[index1 + 11] = particles[i].y + psize*k + startSize*0.5f
            bufferData[index1 + 12] = k
            bufferData[index1 + 13] = 1.0f
            bufferData[index1 + 14] = 1.0f

            bufferData[index1 + 15] = particles[i].x + psize*k + startSize*0.5f
            bufferData[index1 + 16] = particles[i].y - psize*k - startSize*0.5f
            bufferData[index1 + 17] = k
            bufferData[index1 + 18] = 1.0f
            bufferData[index1 + 19] = 0.0f
            index1 += 20
        }
    }

    private fun updateParticlesCircular(factor: Float, psize: Float) {
        var index1 = 0
        for (i in 0 until numParticles) {
            val k = (((i.toFloat() * factor) + tick) % particleLifetime)
            val ax = Math.sin(offsets[i*2].toDouble()).toFloat()
            val ay = Math.cos(offsets[i*2].toDouble()).toFloat()
            val vx = (ax * particleVelocity.x) * particleLifetime
            val vy = (ay * particleVelocity.y) * particleLifetime

            particles[i].x = vx * k + offsets[i*2] + ax * particleSpread * offsets[i*2+1]
            particles[i].y = vy * k + offsets[i*2+1] + ay * particleSpread * offsets[i*2+1]
            particles[i].i = k / particleLifetime
        }

        particles.sortByDescending { p -> p.i }

        for (i in 0 until numParticles) {
            val k = particles[i].i

            bufferData[index1] = particles[i].x - psize*k - startSize*0.5f
            bufferData[index1 + 1] = particles[i].y - psize*k - startSize*0.5f
            bufferData[index1 + 2] = k
            bufferData[index1 + 3] = 0.0f
            bufferData[index1 + 4] = 0.0f

            bufferData[index1 + 5] = particles[i].x - psize*k - startSize*0.5f
            bufferData[index1 + 6] = particles[i].y + psize*k + startSize*0.5f
            bufferData[index1 + 7] = k
            bufferData[index1 + 8] = 0.0f
            bufferData[index1 + 9] = 1.0f

            bufferData[index1 + 10] = particles[i].x + psize*k + startSize*0.5f
            bufferData[index1 + 11] = particles[i].y + psize*k + startSize*0.5f
            bufferData[index1 + 12] = k
            bufferData[index1 + 13] = 1.0f
            bufferData[index1 + 14] = 1.0f

            bufferData[index1 + 15] = particles[i].x + psize*k + startSize*0.5f
            bufferData[index1 + 16] = particles[i].y - psize*k - startSize*0.5f
            bufferData[index1 + 17] = k
            bufferData[index1 + 18] = 1.0f
            bufferData[index1 + 19] = 0.0f
            index1 += 20
        }
    }
}
