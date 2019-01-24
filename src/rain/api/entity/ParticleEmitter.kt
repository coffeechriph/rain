package rain.api.entity

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.gfx.*
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.util.*

class ParticleEmitter internal constructor(
        val entityId: Long,
        material: Material,
        resourceFactory: ResourceFactory,
        val parentTransform: Transform,
        private val numParticles: Int,
        private val particleSize: Float,
        private val particleLifetime: Float,
        private val particleVelocity: Vector2f,
        private val directionType: DirectionType,
        particleSpread: Float,
        private val tickRate: Float = 1.0f) {
    data class Particle (var x: Float, var y: Float, var i: Float)

    private var renderComponent: RenderComponent
    var transform = Transform()
    var startColor = Vector4f(1.0f, 0.0f, 0.0f, 1.0f)
    var endColor = Vector4f(0.0f, 1.0f, 0.0f, 1.0f)
    var startSize = 1.0f
    var enabled: Boolean = true
        set(value) {
            field = value
            renderComponent.visible = value
        }

    private var particles: Array<Particle> = Array(numParticles){ Particle(0.0f, 0.0f, 0.0f) }
    private var bufferData: ByteBuffer = memAlloc(numParticles*12*4)
    private var ibuffer = bufferData.asIntBuffer()
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

        val vertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(bufferData)
                .withState(VertexBufferState.DYNAMIC)
                .withDataType(DataType.INT)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 1))
                .build()

        val indexBuffer = resourceFactory.createIndexBuffer(indices, VertexBufferState.DYNAMIC)
        val mesh = Mesh(vertexBuffer, indexBuffer)
        renderComponent = RenderComponent(transform, mesh, material)
        renderManagerNewRenderComponents.add(renderComponent)

        renderComponent.createUniformData = {
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
            modelMatrixBuffer
        }
    }

    fun destroy() {
        renderManagerRemoveRenderComponents.add(renderComponent)
    }

    fun update() {
        tick += 1.0f / 60.0f * tickRate

        val psize = particleSize * 0.5f
        val factor = particleLifetime / numParticles

        if (directionType == DirectionType.LINEAR) {
            updateParticlesLinear(factor, psize)
        } else {
            throw NotImplementedError("Circular emitters are not implemented!")
        }

        renderComponent.mesh.vertexBuffer.update(bufferData)
    }

    private fun updateParticlesLinear(factor: Float, psize: Float) {
        var index1 = 0
        val vx = particleVelocity.x * particleLifetime
        val vy = particleVelocity.y * particleLifetime

        for (i in 0 until numParticles) {
            val k = (((i.toFloat() * factor) + tick) % particleLifetime)
            particles[i].x = vx * k + offsets[i*2]
            particles[i].y = vy * k + offsets[i*2+1]
            particles[i].i = k
        }

        particles.sortByDescending { p -> p.i }

        for (i in 0 until numParticles) {
            val k = particles[i].i / particleLifetime

            val lx = (particles[i].x - psize * k - startSize * 0.5f).toInt()
            val ly = (particles[i].y - psize * k - startSize * 0.5f).toInt()
            val ry = (particles[i].y + psize * k + startSize * 0.5f).toInt()
            val rx = (particles[i].x + psize * k + startSize * 0.5f).toInt()
            val kz = (k*1000).toInt() shl 2

            ibuffer.put(index1, lx)
            ibuffer.put(index1+1, ly)
            ibuffer.put(index1+2, kz)

            ibuffer.put(index1+3, lx)
            ibuffer.put(index1+4, ry)
            ibuffer.put(index1+5, kz or (1 shl 1))

            ibuffer.put(index1+6, rx)
            ibuffer.put(index1+7, ry)
            ibuffer.put(index1+8, kz or 1 or (1 shl 1))

            ibuffer.put(index1+9, rx)
            ibuffer.put(index1+10, ly)
            ibuffer.put(index1+11, kz or 1)

            index1 += 12
        }
    }
}
