package rain.api.scene

import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBufferState
import rain.api.manager.addNewRenderComponentToRenderer
import rain.api.manager.removeRenderComponentFromRenderer
import rain.assertion
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

class Tilemap internal constructor(){
    private val modelMatrix = Matrix4f()
    var tileNumX = 128
        private set
    var tileNumY = 128
        private set
    var tileWidth = 16.0f
        private set
    var tileHeight = 16.0f
        private set
    var transform = Transform()
        private set
    var visible = true
        set(value) {
            field = value

            if (::renderComponent.isInitialized) {
                renderComponent.visible = value
            }
        }

    private lateinit var renderComponent: RenderComponent
    private lateinit var byteBuffer: ByteBuffer
    private lateinit var resourceFactory: ResourceFactory
    private lateinit var material: Material
    private var vertexData = floatArrayOf()
    private var updateMesh = false

    fun getTileImageIndex(tileX: Int, tileY: Int): Pair<Int, Int> {
        val bufferIndex = (tileX + tileY * tileNumX) * 48
        // Potential rounding errors
        return Pair((vertexData[bufferIndex+2] / material.getTexture2d()[0].getTexCoordWidth()).toInt(),
                    (vertexData[bufferIndex+3] / material.getTexture2d()[0].getTexCoordHeight()).toInt())
    }

    fun setTile(tileX: Int, tileY: Int, imageX: Int, imageY: Int) {
        setTile(tileX, tileY, imageX, imageY, 1.0f, 1.0f, 1.0f, 1.0f)
    }

    fun setTile(tileX: Int, tileY: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        val imageIndex = getTileImageIndex(tileX, tileY)
        setTile(tileX, tileY, imageIndex.first, imageIndex.second, red, green, blue, alpha)
    }

    fun setTile(tileX: Int, tileY: Int, imageX: Int, imageY: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        if (tileX < 0 || tileX > tileNumX || tileY < 0 || tileY > tileNumY) {
            assertion("Tile index out of bounds $tileX, $tileY")
        }

        var bufferIndex = (tileX + tileY * tileNumX) * 48
        val x = tileX.toFloat() * tileWidth
        val y = tileY.toFloat() * tileHeight
        val uvx = material.getTexture2d()[0].getTexCoordWidth()
        val uvy = material.getTexture2d()[0].getTexCoordHeight()

        vertexData[bufferIndex++] = x
        vertexData[bufferIndex++] = y
        vertexData[bufferIndex++] = imageX * uvx
        vertexData[bufferIndex++] = imageY * uvy
        vertexData[bufferIndex++] = red
        vertexData[bufferIndex++] = green
        vertexData[bufferIndex++] = blue
        vertexData[bufferIndex++] = alpha

        vertexData[bufferIndex++] = x
        vertexData[bufferIndex++] = y + tileHeight
        vertexData[bufferIndex++] = imageX * uvx
        vertexData[bufferIndex++] = uvy + imageY * uvy
        vertexData[bufferIndex++] = red
        vertexData[bufferIndex++] = green
        vertexData[bufferIndex++] = blue
        vertexData[bufferIndex++] = alpha

        vertexData[bufferIndex++] = x + tileWidth
        vertexData[bufferIndex++] = y + tileHeight
        vertexData[bufferIndex++] = uvx + imageX * uvx
        vertexData[bufferIndex++] = uvy + imageY * uvy
        vertexData[bufferIndex++] = red
        vertexData[bufferIndex++] = green
        vertexData[bufferIndex++] = blue
        vertexData[bufferIndex++] = alpha

        vertexData[bufferIndex++] = x + tileWidth
        vertexData[bufferIndex++] = y + tileHeight
        vertexData[bufferIndex++] = uvx + imageX * uvx
        vertexData[bufferIndex++] = uvy + imageY * uvy
        vertexData[bufferIndex++] = red
        vertexData[bufferIndex++] = green
        vertexData[bufferIndex++] = blue
        vertexData[bufferIndex++] = alpha

        vertexData[bufferIndex++] = x + tileWidth
        vertexData[bufferIndex++] = y
        vertexData[bufferIndex++] = uvx + imageX * uvx
        vertexData[bufferIndex++] = imageY * uvy
        vertexData[bufferIndex++] = red
        vertexData[bufferIndex++] = green
        vertexData[bufferIndex++] = blue
        vertexData[bufferIndex++] = alpha

        vertexData[bufferIndex++] = x
        vertexData[bufferIndex++] = y
        vertexData[bufferIndex++] = imageX * uvx
        vertexData[bufferIndex++] = imageY * uvy
        vertexData[bufferIndex++] = red
        vertexData[bufferIndex++] = green
        vertexData[bufferIndex++] = blue
        vertexData[bufferIndex] = alpha
        updateMesh = true
    }

    fun removeTile(tileX: Int, tileY: Int) {
        var bufferIndex = (tileX + tileY * tileNumX) * 48
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f

        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f

        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f

        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f

        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f

        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex++] = -1.0f
        vertexData[bufferIndex] = -1.0f
        updateMesh = true
    }

    fun clearTiles() {
        vertexData = FloatArray(tileNumX*tileNumY*48){-1.0f}
    }

    internal fun create(resourceFactory: ResourceFactory, material: Material, tileNumX: Int = 32,
               tileNumY: Int = 32,
               tileWidth: Float = 32.0f, tileHeight: Float = 32.0f) {
        vertexData = FloatArray(tileNumX*tileNumY*48){-1.0f}

        this.tileNumX = tileNumX
        this.tileNumY = tileNumY
        this.tileWidth = tileWidth
        this.tileHeight = tileHeight

        this.resourceFactory = resourceFactory
        this.material = material
    }

    private fun ensureRenderComponentSetup() {
        if (::renderComponent.isInitialized) {
            return
        }

        val vertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(byteBuffer)
                .withState(VertexBufferState.STATIC)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 2))
                .withAttribute(VertexAttribute(2, 4))
                .build()
        val mesh = Mesh(vertexBuffer, null)

        renderComponent = RenderComponent(transform, mesh, material)
        renderComponent.visible = visible
        addNewRenderComponentToRenderer(renderComponent)
        renderComponent.createUniformData = {
            if (transform.updated) {
                modelMatrix.identity()
                modelMatrix.rotateZ(transform.rot)
                modelMatrix.translate(transform.x, transform.y, transform.z)
                modelMatrix.scale(transform.sx, transform.sy, 0.0f)
            }

            val uboBuffer = MemoryUtil.memAlloc(16 * 4)
            modelMatrix.get(uboBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
            uboBuffer
        }
    }

    internal fun updateRenderComponent() {
        if (updateMesh) {
            var bufferSize = 0
            for (i in 0 until vertexData.size step 8) {
                if (vertexData[i+2] < 0.0f) {
                    continue
                }
                bufferSize += 8 * 4
            }

            if (!::byteBuffer.isInitialized) {
                byteBuffer = memAlloc(bufferSize)
            }
            else if (byteBuffer.capacity() != bufferSize) {
                memFree(byteBuffer)
                byteBuffer = memAlloc(bufferSize)
            }

            if (bufferSize > 0) {
                val vertexBuffer = byteBuffer.asFloatBuffer()
                for (i in 0 until vertexData.size step 8) {
                    if (vertexData[i+2] < 0.0f) {
                        continue
                    }

                    vertexBuffer.put(vertexData[i])
                    vertexBuffer.put(vertexData[i+1])
                    vertexBuffer.put(vertexData[i+2])
                    vertexBuffer.put(vertexData[i+3])
                    vertexBuffer.put(vertexData[i+4])
                    vertexBuffer.put(vertexData[i+5])
                    vertexBuffer.put(vertexData[i+6])
                    vertexBuffer.put(vertexData[i+7])
                }
                vertexBuffer.flip()
                ensureRenderComponentSetup()
                if (renderComponent.mesh.vertexBuffer.valid()) {
                    renderComponent.mesh.vertexBuffer.update(byteBuffer)
                }
            }
        }
    }

    internal fun destroy() {
        removeRenderComponentFromRenderer(renderComponent)
    }
}
