package rain.api.gui

class ToggleButton: GuiC() {
    var text = "ToggleButton"

    override fun trigger(): Boolean {
        active = !active
        return true
    }
}
