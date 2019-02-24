package rain.api.gui

class TextField: GuiC() {
    var cursorIndex = 0

    override fun trigger(): Boolean {
        this.active = true
        return true
    }
}
