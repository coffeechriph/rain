package rain.api.gfx

interface ResourceFactory {
    fun createIndexBuffer(indices: IntArray, state: VertexBufferState): IndexBuffer
    fun createTexelBuffer(initialSize: Long): TexelBuffer
    fun buildMaterial(): MaterialBuilder
    fun buildVertexBuffer(): VertexBufferBuilder
    fun buildTexture2d(): Texture2dBuilder
    fun deleteMaterial(name: String)
    fun deleteTexture2d(name: String)
    fun deleteVertexBuffer(vertexBuffer: VertexBuffer)
    fun deleteIndexBuffer(indexBuffer: IndexBuffer)
    fun getMaterial(name: String): Material
    fun getTexture2d(name: String): Texture2d
    fun clear()
}
