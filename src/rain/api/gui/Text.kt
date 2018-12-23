package rain.api.gui

class Text(var string: String, val parent: GuiC?, val container: Container) {
    var x: Float = 0.0f
    var y: Float = 0.0f
    var w: Float = 0.0f
    var h: Float = 0.0f
    var background = false
}
