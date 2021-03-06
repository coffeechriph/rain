package rain.api.components

import org.joml.Matrix4f
import org.joml.Vector2i
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import java.nio.ByteBuffer

class RenderComponent internal constructor(
        internal var transform: Transform,
        internal val mesh: Mesh,
        internal val material: Material){

    // TODO: Maybe move this to the Material
    // Now this is applied in the shader by ADDING with the texture color...
    // This is not really the behaviour one would expect...
    val color = Vector4f(0.0f, 0.0f, 0.0f, 0.0f)
    var visible = true

    // TODO: Material could include a textureOffset in order to move the whole UV mapping in chunks relative to
    // the texture settings
    var textureTileOffset = Vector2i(0, 0)
    private var modelMatrix = Matrix4f()
    private var customUniformDataIndex = 0
    private val customUniformData = FloatArray(10)

    var createUniformData: () -> ByteBuffer = {
        val uniformData = MemoryUtil.memAlloc(32 * 4)
        if (transform.updated) {
            modelMatrix = transform.matrix()
        }

        val buffer = modelMatrix.get(uniformData) ?: throw IllegalStateException("Unable to get matrix content!")
        val ibuf = buffer.asFloatBuffer()
        ibuf.put(16, color.x)
        ibuf.put(17, color.y)
        ibuf.put(18, color.z)
        ibuf.put(19, color.w)
        ibuf.put(20, textureTileOffset.x.toFloat())
        ibuf.put(21, textureTileOffset.y.toFloat())

        var index = 0
        for (custom in customUniformData) {
            ibuf.put(22+index, custom)
            index += 1
        }
        uniformData
    }

    fun addCustomUniformData(offset: Int, vararg data: Float) {
        if (offset+data.size >= customUniformData.size) {
            throw IndexOutOfBoundsException("There can only be a maximum of 10 floats for custom uniform data.")
        }

        var i = 0
        for (d in data) {
            customUniformData[offset+i] = d
            i += 1
        }
    }
}
