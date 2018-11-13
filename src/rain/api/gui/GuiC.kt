package rain.api.gui

open class GuiC {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
    var h = 0.0f
    var active = false
    var text = "Nothing"

    open fun trigger(): Boolean{return true}
}
