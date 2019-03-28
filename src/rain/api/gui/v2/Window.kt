package rain.api.gui.v2

class Window(windowLayout: WindowLayout, font: Font): Panel(windowLayout, font) {
    internal lateinit var closeButton: Button
    internal lateinit var title: Label

    // TODO: Implement window skin
    override fun decoratePanel(depth: Float): FloatArray {
        return gfxCreateRect(x, y, depth, w, 30.0f, skin.buttonStyle.hoverColor) + super.decoratePanel(depth)
    }
}
