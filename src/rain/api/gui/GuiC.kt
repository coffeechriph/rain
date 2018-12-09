package rain.api.gui

open class GuiC() {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
    var h = 0.0f
    var active = false
    var outline = true
        set(value) {
            field = value
            container.isDirty = true
        }
    var outlineWidth = 1
    var text = "Nothing"
    lateinit var container: Container

    open fun trigger(): Boolean{return true}
}
