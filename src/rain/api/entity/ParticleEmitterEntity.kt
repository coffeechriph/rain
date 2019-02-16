package rain.api.entity

import org.joml.Matrix4f
import org.joml.Random
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.components.Transform
import rain.api.gfx.*
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import java.nio.IntBuffer

// TODO: Rewrite particle emitters to support both continous and burst as well as singleFire
// and make them entities instead of components.

class ParticleEmitterEntity internal constructor(resourceFactory: ResourceFactory,
                                                 random: Random,
                                                 private val particleLifetime: Float,
                                                 private val particleMaxSize: Float,
                                                 particleDensity: Int,
                                                 particleSpread: Float) : Entity() {
    var enabled = true
    var velocity = Vector2f(0.0f, -1.0f)
    var startColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
    var endColor = Vector4f(0.0f, 0.0f, 0.0f, 0.0f)
    internal val mesh: Mesh
    private var tick = 0.0f
    private val particleOffsets: FloatArray
    private val particles: Array<Particle>
    private val vertexBuffer: VertexBuffer
    private val indexBuffer: IndexBuffer
    private val byteBufferData: ByteBuffer
    private val intBufferData: IntBuffer

    init {
        getTransform().sx = 1.0f
        getTransform().sy = 1.0f
        getTransform().x = 400.0f
        getTransform().y = 300.0f

        val particleCount = (particleLifetime * 60).toInt() * particleDensity
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
        indexBuffer = resourceFactory.createIndexBuffer(intArrayOf(particleCount*6), VertexBufferState.STATIC)
        mesh = Mesh(vertexBuffer, indexBuffer)
        particles = Array(particleCount){Particle(0.0f, 0.0f, 0.0f)}
        this.particleOffsets = FloatArray(particleCount*2)
        for (i in 0 until this.particleOffsets.size step 2) {
            particleOffsets[i] = (random.nextFloat() - 0.5f) * particleSpread * velocity.x
            particleOffsets[i+1] = (random.nextFloat() - 0.5f) * particleSpread * velocity.y
        }
    }

    internal fun getUniformData(): ByteBuffer {
        val modelMatrix = Matrix4f()
        modelMatrix.identity()
        modelMatrix.rotateZ(transform.rot)
        modelMatrix.translate(transform.x, transform.y, transform.z)
        modelMatrix.scale(transform.sx, transform.sy, 0.0f)

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
            particles[i].x = velocity.x * k + particleOffsets[i*2]
            particles[i].y = velocity.y * k + particleOffsets[i*2+1]
            particles[i].t = k
        }

        particles.sortByDescending { particle -> particle.t }

        val psize = particleMaxSize * 0.5f
        var index1 = 0
        for (i in 0 until particles.size) {
            val k = particles[i].t

            val lx = (particles[i].x - psize * k).toInt()
            val ly = (particles[i].y - psize * k).toInt()
            val ry = (particles[i].y + psize * k).toInt()
            val rx = (particles[i].x + psize * k).toInt()
            val kz = (k*1000).toInt() shl 2

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

        getRenderComponents()[0].mesh.vertexBuffer.update(byteBufferData)
    }
}
