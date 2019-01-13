package rain.api.gfx

typealias createMaterialHandler = (name: String, vertexShaderFile: String, fragmentShaderFile: String, texture2d: Texture2d?, useBatching: Boolean, depthWriteEnabled: Boolean, enableBlend: Boolean, srcColor: BlendMode, dstColor: BlendMode, srcAlpha: BlendMode, dstAlpha: BlendMode) -> Material
class MaterialBuilder internal constructor(val handler: createMaterialHandler) {
    private var texture2d: Texture2d? = null
    private var vertexShaderFile: String = "./data/shaders/basic.vert.spv"
    private var fragmentShaderFile: String = "./data/shaders/basic.frag.spv"
    private var useBatching = false
    private var depthWrite = true
    private var blendEnabled = true
    private var name = "material"
    private var srcColor: BlendMode = BlendMode.BLEND_FACTOR_SRC_ALPHA
    private var dstColor: BlendMode = BlendMode.BLEND_FACTOR_ONE_MINUS_SRC_ALPHA

    fun withName(name: String): MaterialBuilder {
        this.name = name
        return this
    }

    fun withTexture(texture2d: Texture2d): MaterialBuilder {
        this.texture2d = texture2d
        return this
    }

    fun withVertexShader(vertexShaderFile: String): MaterialBuilder {
        this.vertexShaderFile = vertexShaderFile
        return this
    }

    fun withFragmentShader(fragmentShaderFile: String): MaterialBuilder {
        this.fragmentShaderFile = fragmentShaderFile
        return this
    }

    fun withBatching(value: Boolean): MaterialBuilder {
        this.useBatching = value
        return this
    }

    fun withDepthWrite(value: Boolean): MaterialBuilder {
        this.depthWrite = value
        return this
    }

    fun withBlendEnabled(value: Boolean): MaterialBuilder {
        this.blendEnabled = value
        return this
    }

    fun withSrcColor(mode: BlendMode): MaterialBuilder {
        this.srcColor = mode
        return this
    }

    fun withDstColor(mode: BlendMode): MaterialBuilder {
        this.dstColor = mode
        return this
    }

    fun build(): Material {
        val material = handler(name, vertexShaderFile, fragmentShaderFile, texture2d, useBatching, depthWrite, blendEnabled, srcColor, dstColor, BlendMode.BLEND_FACTOR_ONE, BlendMode.BLEND_FACTOR_ZERO)
        reset()
        return material
    }

    private fun reset() {
        texture2d = null
        vertexShaderFile = "./data/shaders/basic.vert.spv"
        fragmentShaderFile = "./data/shaders/basic.frag.spv"
        useBatching = false
        name = "material"
        depthWrite = true
        blendEnabled = true
        srcColor = BlendMode.BLEND_FACTOR_SRC_ALPHA
        dstColor = BlendMode.BLEND_FACTOR_ONE_MINUS_SRC_ALPHA
    }
}