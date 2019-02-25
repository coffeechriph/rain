package rain.api.gui.v2

class Text {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
    var string = ""
        set(value) {
            field = value
            parentPanel.compose = true
        }
    internal var parentComponent: Component? = null
    internal lateinit var parentPanel: Panel
}