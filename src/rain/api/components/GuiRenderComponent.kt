package rain.api.components

import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import java.nio.ByteBuffer

class GuiRenderComponent internal constructor(
        internal val mesh: Mesh,
        internal val material: Material) {
    var visible: Boolean = true
    private val customUniformData = FloatArray(10)

    var createUniformData: () -> ByteBuffer = {
        memAlloc(0)
    }
}
