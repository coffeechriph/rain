package rain.api.gui

class Button: GuiC() {
    override fun trigger(): Boolean {
        active = !active
        return true
    }
}
