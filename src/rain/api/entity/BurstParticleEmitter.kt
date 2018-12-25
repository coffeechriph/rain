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

class BurstParticleEmitter constructor(private val resourceFactory: ResourceFactory, val parentTransform: Transform, private val numParticles: Int, private val
particleSize: Float, private val particleLifetime: Float, private val particleVelocity: Vector2f, private val directionType: DirectionType, private val particleSpread: Float) {
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
    var singleBurst = false
    var burstFinished = false
    var particlesPerBurst = 1

    private var simIndex = 0
    private var simStartIndex = 0
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

        for (i in 0 until particles.size) {
            particles[i].x = offsets[i*2]
            particles[i].y = offsets[i*2+1]
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

        vertexBuffer = resourceFactory.createVertexBuffer(bufferData, VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 3), VertexAttribute(1, 2)))
        indexBuffer = resourceFactory.createIndexBuffer(indices, VertexBufferState.DYNAMIC)
    }

    fun clear() {
        resourceFactory.deleteVertexBuffer(vertexBuffer)
        resourceFactory.deleteIndexBuffer(indexBuffer)
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

    fun update(entitySystem: EntitySystem<Entity>, deltaTime: Float) {
        tick += deltaTime

        val psize = particleSize * 0.5f / particleLifetime
        val factor = particleLifetime / numParticles

        if (directionType == DirectionType.LINEAR) {
            updateParticlesLinear(factor, psize)
        } else {
            updateParticlesCircular(factor, psize)
        }

        vertexBuffer.update(bufferData)
    }

    fun fireSingleBurst() {
        if (singleBurst && burstFinished) {
            for (i in 0 until particles.size) {
                particles[i].x = offsets[i*2]
                particles[i].y = offsets[i*2+1]
                particles[i].i = 0.0f
            }
            simIndex = 0
            simStartIndex = 0
            burstFinished = false
        }
    }

    // TODO: Velocity should be actual velocity and not the amount to travel during the lifetime
    private fun updateParticlesLinear(factor: Float, psize: Float) {
        if (burstFinished) {
            return
        }

        var index1 = 0
        val vx = particleVelocity.x
        val vy = particleVelocity.y

        if (singleBurst) {
            simulateSingleBurstLinear(vx, vy)
        }
        else {
            simulateContinousBurstLinear(vx, vy)
        }

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

    private fun simulateSingleBurstLinear(vx: Float, vy: Float) {
        for (i in simStartIndex until simIndex) {
            particles[i].x += vx * 0.016f
            particles[i].y += vy * 0.016f
            particles[i].i += 0.016f

            if (particles[i].i >= particleLifetime) {
                particles[i].x = Float.MAX_VALUE
                particles[i].y = Float.MAX_VALUE
                particles[i].i = 0.0f
                simStartIndex += 1
            }
        }

        if (simIndex < particles.size) {
            simIndex += particlesPerBurst
        }

        if (simIndex in 1..simStartIndex) {
            burstFinished = true
        }
    }

    private fun simulateContinousBurstLinear(vx: Float, vy: Float) {
        for (i in 0 until simIndex) {
            particles[i].x += vx * 0.016f
            particles[i].y += vy * 0.016f
            particles[i].i += 0.016f

            if (particles[i].i >= particleLifetime) {
                particles[i].x = offsets[i*2]
                particles[i].y = offsets[i*2+1]
                particles[i].i = 0.0f
            }
        }

        if (simIndex < particles.size) {
            simIndex += particlesPerBurst
        }
    }

    private fun updateParticlesCircular(factor: Float, psize: Float) {
        var index1 = 0

        if (singleBurst) {
            simulateSingleBurstCircular(factor)
        }
        else {
            simulateContinousBurstCircular(factor)
        }

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

    private fun simulateContinousBurstCircular(factor: Float) {
        for (i in 0 until simIndex) {
            val ax = Math.sin(offsets[i * 2].toDouble()).toFloat()
            val ay = Math.cos(offsets[i * 2].toDouble()).toFloat()
            val vx = (ax * particleVelocity.x)
            val vy = (ay * particleVelocity.y)

            particles[i].x += vx
            particles[i].y += vy
            particles[i].i += 0.016f

            if (particles[i].i >= particleLifetime) {
                particles[i].x = offsets[i*2]
                particles[i].y = offsets[i*2+1]
                particles[i].i = 0.0f
            }
        }

        if (simIndex < particles.size) {
            simIndex += particlesPerBurst
        }
    }

    private fun simulateSingleBurstCircular(factor: Float) {
        for (i in simStartIndex until simIndex) {
            val ax = Math.sin(offsets[i * 2].toDouble()).toFloat()
            val ay = Math.cos(offsets[i * 2].toDouble()).toFloat()
            val vx = (ax * particleVelocity.x)
            val vy = (ay * particleVelocity.y)

            particles[i].x += vx
            particles[i].y += vy
            particles[i].i += 0.016f

            if (particles[i].i >= particleLifetime) {
                particles[i].x = Float.MAX_VALUE
                particles[i].y = Float.MAX_VALUE
                particles[i].i = 0.0f
                simStartIndex += 1
            }
        }

        if (simIndex < particles.size) {
            simIndex += particlesPerBurst
        }

        if (simStartIndex >= simIndex) {
            burstFinished = true
        }
    }
}