package rain.api.gfx

interface Material {
    fun useBatching(): Boolean
    fun getTexelBuffer(): TexelBuffer
    fun getTexture2d(): Array<Texture2d>
    fun getName(): String
    fun valid(): Boolean
    fun copy(): Material
}
