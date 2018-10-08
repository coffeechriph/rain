package rain.api

class Tilemap {
    data class TileIndex(val x: Int, val y: Int)

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
    internal lateinit var material: Material
        private set
    internal var transform = Transform()
        private set

    private lateinit var vertices: FloatArray

    fun create(resourceFactory: ResourceFactory, material: Material, tileNumX: Int = 32, tileNumY: Int = 32,
               tileWidth: Float = 32.0f, tileHeight: Float = 32.0f, map: Array<TileIndex>) {
        assert(map.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        vertices = FloatArray(tileNumX*tileNumY*4*6) // pos(vec2), uv(vec2). 6 vertices per tile
        var vi: Int
        for (i in 0 until tileNumX*tileNumY) {
            vi = 24*i
            val tileX = map[i].x
            val tileY = map[i].y
            val uvx = material.getTexture2d().getTexCoordWidth()
            val uvy = material.getTexture2d().getTexCoordHeight()

            vertices[vi+0] = x
            vertices[vi+1] = y
            vertices[vi+2] = tileX * uvx
            vertices[vi+3] = tileY * uvy

            vertices[vi+4] = x
            vertices[vi+5] = y + tileHeight
            vertices[vi+6] = tileX * uvx
            vertices[vi+7] = uvy + tileY * uvy

            vertices[vi+8] = x + tileWidth
            vertices[vi+9] = y + tileHeight
            vertices[vi+10] = uvx + tileX * uvx
            vertices[vi+11] = uvy + tileY * uvy

            vertices[vi+12] = x + tileWidth
            vertices[vi+13] = y + tileHeight
            vertices[vi+14] = uvx + tileX * uvx
            vertices[vi+15] = uvy + tileY * uvy

            vertices[vi+16] = x + tileWidth
            vertices[vi+17] = y
            vertices[vi+18] = uvx + tileX * uvx
            vertices[vi+19] = tileY * uvy

            vertices[vi+20] = x
            vertices[vi+21] = y
            vertices[vi+22] = tileX * uvx
            vertices[vi+23] = tileY * uvy

            x += tileWidth
            if (x >= tileNumX * tileWidth) {
                x = 0.0f
                y += tileHeight
            }
        }

        vertexBuffer = resourceFactory.createVertexBuffer(vertices, VertexBufferState.STATIC)
        this.tileNumX = tileNumX
        this.tileNumY = tileNumY
        this.tileWidth = tileWidth
        this.tileHeight = tileHeight
        this.material = material
    }

    // TODO: Add a check to make sure that the vertexBuffer has already been created
    fun update(tileIndices: Array<TileIndex>) {
        assert(tileIndices.size == tileNumX * tileNumY)

        var x = 0.0f
        var y = 0.0f

        vertices = FloatArray(tileNumX*tileNumY*4*6) // pos(vec2), uv(vec2). 6 vertices per tile
        var vi: Int
        for (i in 0 until tileNumX*tileNumY) {
            vi = 24*i
            val tileX = tileIndices[i].x
            val tileY = tileIndices[i].y
            val uvx = material.getTexture2d().getTexCoordWidth()
            val uvy = material.getTexture2d().getTexCoordHeight()

            vertices[vi+0] = x
            vertices[vi+1] = y
            vertices[vi+2] = tileX * uvx
            vertices[vi+3] = tileY * uvy

            vertices[vi+4] = x
            vertices[vi+5] = y + tileHeight
            vertices[vi+6] = tileX * uvx
            vertices[vi+7] = uvy + tileY * uvy

            vertices[vi+8] = x + tileWidth
            vertices[vi+9] = y + tileHeight
            vertices[vi+10] = uvx + tileX * uvx
            vertices[vi+11] = uvy + tileY * uvy

            vertices[vi+12] = x + tileWidth
            vertices[vi+13] = y + tileHeight
            vertices[vi+14] = uvx + tileX * uvx
            vertices[vi+15] = uvy + tileY * uvy

            vertices[vi+16] = x + tileWidth
            vertices[vi+17] = y
            vertices[vi+18] = uvx + tileX * uvx
            vertices[vi+19] = tileY * uvy

            vertices[vi+20] = x
            vertices[vi+21] = y
            vertices[vi+22] = tileX * uvx
            vertices[vi+23] = tileY * uvy

            x += tileWidth
            if (x >= tileNumX * tileWidth) {
                x = 0.0f
                y += tileHeight
            }
        }

        vertexBuffer.update(vertices)
    }
}
