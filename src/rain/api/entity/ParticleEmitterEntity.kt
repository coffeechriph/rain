package rain.api.entity

import org.joml.Random
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.gfx.*
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.sign

class ParticleEmitterEntity internal constructor(resourceFactory: ResourceFactory,
                                                 random: Random,
                                                 private val particleLifetime: Float,
                                                 particleCount: Int,
                                                 particleSpread: Float) : Entity() {
    var enabled = true
        set(value) {
            getRenderComponents()[0].visible = value
            field = value
        }
    var velocity = Vector2f(0.0f, -1.0f)
    var startColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
    var endColor = Vector4f(0.0f, 0.0f, 1.0f, 0.0f)
    internal val mesh: Mesh
    private var tick = 0.0f
    private val particleOffsets: FloatArray
    private val particles: Array<Particle>
    private val vertexBuffer: VertexBuffer
    private val indexBuffer: IndexBuffer
    private val byteBufferData: ByteBuffer
    private val intBufferData: IntBuffer

    init {
        byteBufferData = memAlloc(particleCount * 3 * 4 * 4)
        intBufferData = byteBufferData.asIntBuffer()
        vertexBuffer = resourceFactory.buildVertexBuffer()
                .withDataType(DataType.INT)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 1))
                .withState(VertexBufferState.DYNAMIC)
                .withVertices(byteBufferData)
                .build()
        val indices = IntArray(particleCount*6)
        var index = 0
        var triangleIndex = 0
        for (i in 0 until particleCount) {
            indices[index] = triangleIndex
            indices[index+1] = triangleIndex+1
            indices[index+2] = triangleIndex+2

            indices[index+3] = triangleIndex+2
            indices[index+4] = triangleIndex+3
            indices[index+5] = triangleIndex
            index += 6
            triangleIndex += 4
        }
        indexBuffer = resourceFactory.createIndexBuffer(indices, VertexBufferState.STATIC)
        mesh = Mesh(vertexBuffer, indexBuffer)
        particles = Array(particleCount){Particle(0.0f, 0.0f, 0.0f)}
        this.particleOffsets = FloatArray(particleCount*2)
        for (i in 0 until this.particleOffsets.size step 2) {
            particleOffsets[i] = (random.nextFloat() - 0.5f) * particleSpread * velocity.y.sign
            particleOffsets[i+1] = (random.nextFloat() - 0.5f) * particleSpread * velocity.x.sign
        }
    }

    internal fun getUniformData(): ByteBuffer {
        val modelMatrix = transform.matrix()

        val modelMatrixBuffer = memAlloc(24 * 4)
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

    fun simulate() {
        if (!enabled) {
            return
        }

        tick += 1.0f / 60.0f
        val factor = particleLifetime / particles.size
        for (i in 0 until particles.size) {
            val k = (((i.toFloat() * factor) + tick) % particleLifetime)
            particles[i].x = velocity.x * particleLifetime * k + particleOffsets[i*2]
            particles[i].y = velocity.y * particleLifetime * k + particleOffsets[i*2+1]
            particles[i].t = k
        }

        particles.sortByDescending { particle -> particle.t }

        val psize = 0.5f
        var index1 = 0
        for (i in 0 until particles.size) {
            val k = particles[i].t

            val lx = (particles[i].x - psize * k * 100).toInt()
            val ly = (particles[i].y - psize * k * 100).toInt()
            val ry = (particles[i].y + psize * k * 100).toInt()
            val rx = (particles[i].x + psize * k * 100).toInt()
            val kz = ((k/particleLifetime)*1000).toInt() shl 2

            intBufferData.put(index1, lx)
            intBufferData.put(index1+1, ly)
            intBufferData.put(index1+2, kz)

            intBufferData.put(index1+3, lx)
            intBufferData.put(index1+4, ry)
            intBufferData.put(index1+5, kz or (1 shl 1))

            intBufferData.put(index1+6, rx)
            intBufferData.put(index1+7, ry)
            intBufferData.put(index1+8, kz or 1 or (1 shl 1))

            intBufferData.put(index1+9, rx)
            intBufferData.put(index1+10, ly)
            intBufferData.put(index1+11, kz or 1)

            index1 += 12
        }

        vertexBuffer.update(byteBufferData)
    }
}
