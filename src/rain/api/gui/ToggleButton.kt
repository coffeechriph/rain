package rain.api.gui

class ToggleButton(): GuiC() {
    override fun trigger(): Boolean {
        active = !active
        parent.isDirty = true
        return true
    }
}
