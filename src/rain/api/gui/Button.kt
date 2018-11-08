package rain.api.gui

class Button: GuiC() {
    var text = "Button"

    override fun trigger(): Boolean {
        active = !active
        return true
    }
}
