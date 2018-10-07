package rain.api

interface Texture2d {
    fun getWidth(): Int
    fun getHeight(): Int
    fun setTiledTexture(tileWidth: Int, tileHeight: Int)

    fun getTexCoordWidth(): Float
    fun getTexCoordHeight(): Float
}