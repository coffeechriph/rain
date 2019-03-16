package rain.api.scene

import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBufferState
import rain.api.manager.addNewRenderComponentToRenderer
import rain.api.manager.removeRenderComponentFromRenderer
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

class Tilemap {
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

    private lateinit var renderComponent: RenderComponent
    private lateinit var byteBuffer: ByteBuffer

    fun create(resourceFactory: ResourceFactory, material: Material, tileNumX: Int = 32, tileNumY: Int = 32,
               tileWidth: Float = 32.0f, tileHeight: Float = 32.0f, map: Array<TileGfx>) {
        assert(map.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        if (!::byteBuffer.isInitialized || map.size * 48 * 4 > byteBuffer.capacity()) {
            byteBuffer = memAlloc(map.size * 48 * 4)
        }

        val vertices = byteBuffer.asFloatBuffer()
        var bufferIndex = 0
        // TODO: We can optimize this by specifying x,y as 1 int32 and scale them in the shader by size
        // This would allow us to have a tilemap with 65535 tiles on every axis
        for (i in 0 until tileNumX*tileNumY) {
            if (map[i] != TileGfxNone) {
                val tileX = map[i].x
                val tileY = map[i].y
                val red = map[i].red
                val green = map[i].green
                val blue = map[i].blue
                val alpha = map[i].alpha
                val uvx = material.getTexture2d()[0].getTexCoordWidth()
                val uvy = material.getTexture2d()[0].getTexCoordHeight()

                vertices.put(bufferIndex++, x)
                vertices.put(bufferIndex++, y)
                vertices.put(bufferIndex++, tileX * uvx)
                vertices.put(bufferIndex++, tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x)
                vertices.put(bufferIndex++, y + tileHeight)
                vertices.put(bufferIndex++, tileX * uvx)
                vertices.put(bufferIndex++, uvy + tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x + tileWidth)
                vertices.put(bufferIndex++, y + tileHeight)
                vertices.put(bufferIndex++, uvx + tileX * uvx)
                vertices.put(bufferIndex++, uvy + tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x + tileWidth)
                vertices.put(bufferIndex++, y + tileHeight)
                vertices.put(bufferIndex++, uvx + tileX * uvx)
                vertices.put(bufferIndex++, uvy + tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x + tileWidth)
                vertices.put(bufferIndex++, y)
                vertices.put(bufferIndex++, uvx + tileX * uvx)
                vertices.put(bufferIndex++, tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x)
                vertices.put(bufferIndex++, y)
                vertices.put(bufferIndex++, tileX * uvx)
                vertices.put(bufferIndex++, tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)
            }

            x += tileWidth
            if (x >= tileNumX * tileWidth) {
                x = 0.0f
                y += tileHeight
            }
        }

        val vertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(byteBuffer)
                .withState(VertexBufferState.STATIC)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 2))
                .withAttribute(VertexAttribute(2, 4))
                .build()
        val mesh = Mesh(vertexBuffer, null)

        this.tileNumX = tileNumX
        this.tileNumY = tileNumY
        this.tileWidth = tileWidth
        this.tileHeight = tileHeight

        renderComponent = RenderComponent(transform, mesh, material)
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

    fun update(tileGfx: Array<TileGfx>) {
        assert(tileGfx.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        if (!::byteBuffer.isInitialized || tileGfx.size * 48 * 4 > byteBuffer.capacity()) {
            byteBuffer = memAlloc(tileGfx.size * 48 * 4)
        }

        val vertices = byteBuffer.asFloatBuffer()
        var bufferIndex = 0

        for (i in 0 until tileNumX*tileNumY) {
            if (tileGfx[i] != TileGfxNone) {
                val tileX = tileGfx[i].x
                val tileY = tileGfx[i].y
                val red = tileGfx[i].red
                val green = tileGfx[i].green
                val blue = tileGfx[i].blue
                val alpha = tileGfx[i].alpha
                val uvx = renderComponent.material.getTexture2d()[0].getTexCoordWidth()
                val uvy = renderComponent.material.getTexture2d()[0].getTexCoordHeight()

                vertices.put(bufferIndex++, x)
                vertices.put(bufferIndex++, y)
                vertices.put(bufferIndex++, tileX * uvx)
                vertices.put(bufferIndex++, tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x)
                vertices.put(bufferIndex++, y + tileHeight)
                vertices.put(bufferIndex++, tileX * uvx)
                vertices.put(bufferIndex++, uvy + tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x + tileWidth)
                vertices.put(bufferIndex++, y + tileHeight)
                vertices.put(bufferIndex++, uvx + tileX * uvx)
                vertices.put(bufferIndex++, uvy + tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x + tileWidth)
                vertices.put(bufferIndex++, y + tileHeight)
                vertices.put(bufferIndex++, uvx + tileX * uvx)
                vertices.put(bufferIndex++, uvy + tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x + tileWidth)
                vertices.put(bufferIndex++, y)
                vertices.put(bufferIndex++, uvx + tileX * uvx)
                vertices.put(bufferIndex++, tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)

                vertices.put(bufferIndex++, x)
                vertices.put(bufferIndex++, y)
                vertices.put(bufferIndex++, tileX * uvx)
                vertices.put(bufferIndex++, tileY * uvy)
                vertices.put(bufferIndex++, red)
                vertices.put(bufferIndex++, green)
                vertices.put(bufferIndex++, blue)
                vertices.put(bufferIndex++, alpha)
            }

            x += tileWidth
            if (x >= tileNumX * tileWidth) {
                x = 0.0f
                y += tileHeight
            }
        }

        if (renderComponent.mesh.vertexBuffer.valid()) {
            renderComponent.mesh.vertexBuffer.update(byteBuffer)
        }
    }

    fun destroy() {
        removeRenderComponentFromRenderer(renderComponent)
    }
}
