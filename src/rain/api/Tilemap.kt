package rain.api

class Tilemap {
    var tileNumX = 128
    var tileNumY = 128
    var tileWidth = 16
    var tileHeight = 16
    private lateinit var vertices: FloatArray
    internal lateinit var vertexBuffer: VertexBuffer
        private set
    internal lateinit var texture: Texture2d

    // TODO: Implement support for specifying tile type
    // TODO: We want to be able to set a material for a tilemap
    fun create(resourceFactory: ResourceFactory, texture2d: Texture2d, tileNumX: Int, tileNumY: Int, tileWidth: Int, tileHeight: Int) {
        var x = 0.0f
        var y = 0.0f

        vertices = FloatArray(tileNumX*tileNumY*4*6) // pos(vec2), uv(vec2). 6 vertices per tile
        var vi: Int
        for (i in 0 until tileNumX*tileNumY) {
            vi = 24*i
            val tileX = 0
            val tileY = 0
            val uvx = texture2d.getTexCoordWidth()
            val uvy = texture2d.getTexCoordHeight()

            vertices[vi+0] = x
            vertices[vi+1] = y
            vertices[vi+2] = tileX * uvx
            vertices[vi+3] = tileY * uvy

            vertices[vi+4] = x
            vertices[vi+5] = y + 0.5f
            vertices[vi+6] = tileX * uvx
            vertices[vi+7] = tileY + tileY * uvy

            vertices[vi+8] = x + 0.5f
            vertices[vi+9] = y + 0.5f
            vertices[vi+10] = tileX + tileX * uvx
            vertices[vi+11] = tileY + tileY * uvy

            vertices[vi+12] = x + 0.5f
            vertices[vi+13] = y + 0.5f
            vertices[vi+14] = tileX + tileX * uvx
            vertices[vi+15] = tileY + tileY * uvy

            vertices[vi+16] = x + 0.5f
            vertices[vi+17] = y
            vertices[vi+18] = tileX + tileX * uvx
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
        this.texture = texture
    }
}