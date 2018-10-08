package rain.api

class Tilemap {
    var tileNumX = 128
    var tileNumY = 128
    var tileWidth = 16.0f
    var tileHeight = 16.0f
    private lateinit var vertices: FloatArray
    internal lateinit var vertexBuffer: VertexBuffer
        private set
    internal lateinit var material: Material
    internal var transform = Transform()

    // TODO: Implement support for specifying tile type
    fun create(resourceFactory: ResourceFactory, material: Material, tileNumX: Int, tileNumY: Int, tileWidth: Float, tileHeight: Float) {
        var x = 0.0f
        var y = 0.0f

        vertices = FloatArray(tileNumX*tileNumY*4*6) // pos(vec2), uv(vec2). 6 vertices per tile
        var vi: Int
        for (i in 0 until tileNumX*tileNumY) {
            vi = 24*i
            val tileX = 0
            val tileY = 0
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

        vertexBuffer = resourceFactory.createVertexBuffer(vertices)
        this.tileNumX = tileNumX
        this.tileNumY = tileNumY
        this.tileWidth = tileWidth
        this.tileHeight = tileHeight
        this.material = material
    }
}
