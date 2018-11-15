package rain.api.gui

class ToggleButton(): GuiC() {
    override fun trigger(): Boolean {
        active = !active
        container.isDirty = true
        return true
    }
}
