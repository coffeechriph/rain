package rain.api.scene

import org.joml.Matrix4f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.entity.RenderComponent
import rain.api.entity.Transform
import rain.api.gfx.*
import rain.log
import rain.vulkan.VertexAttribute

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

    fun create(resourceFactory: ResourceFactory, material: Material, tileNumX: Int = 32, tileNumY: Int = 32,
               tileWidth: Float = 32.0f, tileHeight: Float = 32.0f, map: Array<TileIndex>) {
        assert(map.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        val vertices = ArrayList<Float>() // pos(vec2), uv(vec2). 6 vertices per tile
        // TODO: We can optimize this by specifying x,y as 1 int32 and scale them in the shader by size
        // This would allow us to have a tilemap with 65535 tiles on every axis
        for (i in 0 until tileNumX*tileNumY) {
            if (map[i] != TileIndexNone) {
                val tileX = map[i].x
                val tileY = map[i].y
                val uvx = material.getTexture2d()[0].getTexCoordWidth()
                val uvy = material.getTexture2d()[0].getTexCoordHeight()

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

        log("Created tilemap mesh with ${vertices.size}/${map.size * 4 * 6} vertices actually allocated.")
        // TODO: Optimize this!
        val byteBuffer = memAlloc(vertices.size*4)
        byteBuffer.asFloatBuffer().put(vertices.toFloatArray()).flip()
        val vertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(byteBuffer)
                .withState(VertexBufferState.STATIC)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 2))
                .build()
        val mesh = Mesh(vertexBuffer, null)

        this.tileNumX = tileNumX
        this.tileNumY = tileNumY
        this.tileWidth = tileWidth
        this.tileHeight = tileHeight

        renderComponent = RenderComponent(transform, mesh, material)
        renderManagerNewRenderComponents.add(renderComponent)
        renderComponent.createUniformData = {
            if (transform.updated) {
                modelMatrix.identity()
                modelMatrix.rotateZ(transform.rot)
                modelMatrix.translate(transform.x, transform.y, transform.z)
                modelMatrix.scale(transform.sx, transform.sy, 0.0f)
            }

            val byteBuffer = MemoryUtil.memAlloc(16 * 4)
            modelMatrix.get(byteBuffer) ?: throw IllegalStateException("Unable to get matrix content!")
            byteBuffer
        }
    }

    fun update(tileIndices: Array<TileIndex>) {
        assert(tileIndices.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        val vertices = ArrayList<Float>() // pos(vec2), uv(vec2). 6 vertices per tile
        for (i in 0 until tileNumX*tileNumY) {
            if (tileIndices[i] != TileIndexNone) {
                val tileX = tileIndices[i].x
                val tileY = tileIndices[i].y
                val uvx = renderComponent.material.getTexture2d()[0].getTexCoordWidth()
                val uvy = renderComponent.material.getTexture2d()[0].getTexCoordHeight()

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

        if (renderComponent.mesh.vertexBuffer.valid()) {
            // TODO: Optimize this
            val byteBuffer = memAlloc(vertices.size*4)
            byteBuffer.asFloatBuffer().put(vertices.toFloatArray()).flip()
            renderComponent.mesh.vertexBuffer.update(byteBuffer)
        }
    }

    fun destroy() {
        renderManagerRemoveRenderComponents.add(renderComponent)
    }
}
