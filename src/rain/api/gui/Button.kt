package rain.api.gui

class Button: GuiC() {
    override fun trigger(): Boolean {
        active = !active
        container.isDirty = true
        return true
    }
}
