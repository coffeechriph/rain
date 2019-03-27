package rain.api.gui.v2

import org.joml.Vector4f

class Text {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
        private set
    var string = ""
        set(value) {
            if (field != value) {
                w = parentPanel.font.getStringWidth(value, 0, value.length)
                parentPanel.compose = true
                field = value
            }
        }
    var visible = true
    internal var color: Vector4f = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
    internal var textAlign = TextAlign.CENTER
    internal var parentComponent: Component? = null
    internal lateinit var parentPanel: Panel
}
