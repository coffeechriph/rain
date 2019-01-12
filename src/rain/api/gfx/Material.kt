package rain.api.gfx

interface Material {
    fun getTexelBuffer(): TexelBuffer
    fun getTexture2d(): Array<Texture2d>
    fun valid(): Boolean
}
