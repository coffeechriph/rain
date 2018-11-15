package rain.api.gui

class Text(var string: String, val parent: GuiC?, val container: Container) {
    var x: Float = 0.0f
        set(value) {
            field = value
            container.isDirty = true
        }
    var y: Float = 0.0f
        set(value) {
            field = value
            container.isDirty = true
        }
    var w: Float = 0.0f
    var h: Float = 0.0f
    var background = false
        set(value) {
            field = value
            container.isDirty = true
        }
}
