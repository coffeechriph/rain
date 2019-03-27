package rain.api.gui.v2

class Window(windowLayout: WindowLayout, font: Font): Panel(windowLayout, font) {
    internal lateinit var closeButton: Button
    internal lateinit var title: Label
}
