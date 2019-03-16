package rain.api.gfx

interface Texture2d {
    fun getWidth(): Int
    fun getHeight(): Int
    fun setTiledTexture(tileWidth: Int, tileHeight: Int)

    fun getTileWidthPixels(): Int
    fun getTileHeightPixels(): Int
    fun getTexCoordWidth(): Float
    fun getTexCoordHeight(): Float
    fun valid(): Boolean
}
