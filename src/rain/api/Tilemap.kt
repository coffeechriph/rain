package rain.api

import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.assertion
import rain.log
import java.nio.ByteBuffer

class Tilemap: Drawable() {
    private val modelMatrix = Matrix4f()

    override fun getMaterial(): Material {
        return material
    }

    override fun getTransform(): Transform {
        return transform
    }

    override fun getStreamedUniformData(): ByteBuffer {
        if (transform.updated) {
            modelMatrix.identity()
            modelMatrix.rotateZ(transform.rot)
            modelMatrix.translate(transform.x, transform.y, transform.z)
            modelMatrix.scale(transform.sx, transform.sy, 0.0f)
        }

        val byteBuffer = MemoryUtil.memAlloc(16 * 4)
        modelMatrix.get(byteBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
        return byteBuffer
    }

    var tileNumX = 128
        private set
    var tileNumY = 128
        private set
    var tileWidth = 16.0f
        private set
    var tileHeight = 16.0f
        private set

    internal lateinit var vertexBuffer: VertexBuffer
        private set
    private lateinit var material: Material
    private var transform = Transform()

    private lateinit var vertices: ArrayList<Float>

    fun create(resourceFactory: ResourceFactory, material: Material, tileNumX: Int = 32, tileNumY: Int = 32,
               tileWidth: Float = 32.0f, tileHeight: Float = 32.0f, map: Array<TileIndex>) {
        assert(map.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        vertices = ArrayList() // pos(vec2), uv(vec2). 6 vertices per tile
        for (i in 0 until tileNumX*tileNumY) {
            if (map[i] != TileIndexNone) {
                val tileX = map[i].x
                val tileY = map[i].y
                val uvx = material.getTexture2d().getTexCoordWidth()
                val uvy = material.getTexture2d().getTexCoordHeight()

                vertices.add(x)
                vertices.add(y)
                vertices.add(tileX * uvx)
                vertices.add(tileY * uvy)

                vertices.add(x)
                vertices.add(y + tileHeight)
                vertices.add(tileX * uvx)
                vertices.add(uvy + tileY * uvy)

                vertices.add(x + tileWidth)
                vertices.add(y + tileHeight)
                vertices.add(uvx + tileX * uvx)
                vertices.add(uvy + tileY * uvy)

                vertices.add(x + tileWidth)
                vertices.add(y + tileHeight)
                vertices.add(uvx + tileX * uvx)
                vertices.add(uvy + tileY * uvy)

                vertices.add(x + tileWidth)
                vertices.add(y)
                vertices.add(uvx + tileX * uvx)
                vertices.add(tileY * uvy)

                vertices.add(x)
                vertices.add(y)
                vertices.add(tileX * uvx)
                vertices.add(tileY * uvy)
            }

            x += tileWidth
            if (x >= tileNumX * tileWidth) {
                x = 0.0f
                y += tileHeight
            }
        }

        log("Created tilemap mesh with ${vertices.size}/${map.size*4*6} vertices actually allocated.")
        vertexBuffer = resourceFactory.createVertexBuffer(vertices.toFloatArray(), VertexBufferState.STATIC)
        this.tileNumX = tileNumX
        this.tileNumY = tileNumY
        this.tileWidth = tileWidth
        this.tileHeight = tileHeight
        this.material = material
    }

    // TODO: Add a check to make sure that the vertexBuffer has already been created
    fun update(resourceFactory: ResourceFactory, tileIndices: Array<TileIndex>) {
        assert(tileIndices.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        vertices = ArrayList() // pos(vec2), uv(vec2). 6 vertices per tile
        for (i in 0 until tileNumX*tileNumY) {
            if (tileIndices[i] != TileIndexNone) {
                val tileX = tileIndices[i].x
                val tileY = tileIndices[i].y
                val uvx = material.getTexture2d().getTexCoordWidth()
                val uvy = material.getTexture2d().getTexCoordHeight()

                vertices.add(x)
                vertices.add(y)
                vertices.add(tileX * uvx)
                vertices.add(tileY * uvy)

                vertices.add(x)
                vertices.add(y + tileHeight)
                vertices.add(tileX * uvx)
                vertices.add(uvy + tileY * uvy)

                vertices.add(x + tileWidth)
                vertices.add(y + tileHeight)
                vertices.add(uvx + tileX * uvx)
                vertices.add(uvy + tileY * uvy)

                vertices.add(x + tileWidth)
                vertices.add(y + tileHeight)
                vertices.add(uvx + tileX * uvx)
                vertices.add(uvy + tileY * uvy)

                vertices.add(x + tileWidth)
                vertices.add(y)
                vertices.add(uvx + tileX * uvx)
                vertices.add(tileY * uvy)

                vertices.add(x)
                vertices.add(y)
                vertices.add(tileX * uvx)
                vertices.add(tileY * uvy)
            }

            x += tileWidth
            if (x >= tileNumX * tileWidth) {
                x = 0.0f
                y += tileHeight
            }
        }

        if (vertices.size > 0) {
            vertexBuffer.update(vertices.toFloatArray())
        }
    }
}
