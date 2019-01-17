package rain.api.gfx

import java.nio.ByteBuffer

typealias createTexture2dHandler = (name: String, imageData: ByteBuffer, width: Int, height: Int, channels: Int, filter: TextureFilter) -> Texture2d
typealias loadTexture2dHandler = (name: String, textureFile: String, filter: TextureFilter) -> Texture2d
class Texture2dBuilder internal constructor(private val createHandler: createTexture2dHandler, private val loadHandler: loadTexture2dHandler){
    private var name = "texture2d"
    private var imageData: ByteBuffer? = null
    private var imageFile: String? = null
    private var width: Int = -1
    private var height: Int = -1
    private var channels: Int = 0
    private var filter: TextureFilter = TextureFilter.NEAREST

    fun withName(name: String): Texture2dBuilder {
        this.name = name
        return this
    }

    fun fromImageData(imageData: ByteBuffer, width: Int, height: Int, channels: Int): Texture2dBuilder {
        this.imageData = imageData
        this.width = width
        this.height = height
        this.channels = channels
        return this
    }

    fun fromImageFile(imageFile: String): Texture2dBuilder {
        this.imageFile = imageFile
        return this
    }

    fun withFilter(filter: TextureFilter): Texture2dBuilder {
        this.filter = filter
        return this
    }

    fun build(): Texture2d {
        if (imageData == null && imageFile == null) {
            throw AssertionError("Must specify source when building a texture 2d!")
        }

        val image = if (imageData != null) {
            createHandler(name, imageData!!, width, height, channels, filter)
        }
        else {
            loadHandler(name, imageFile!!, filter)
        }

        reset()
        return image
    }

    private fun reset() {
        name = "texture2d"
        imageData = null
        imageFile = null
        width = -1
        height = -1
        channels = 0
        filter = TextureFilter.NEAREST
    }
}