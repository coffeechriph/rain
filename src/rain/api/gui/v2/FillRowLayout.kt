package rain.api.gui.v2

class FillRowLayout: Layout() {
    var componentsPerRow = 1
    var componentHeight = 30.0f
    override fun manage(panel: Panel, components: List<Component>) {
        val cw = (panel.w - panel.skin.panelStyle.outlineWidth*2) / componentsPerRow
        var x = panel.x + panel.skin.panelStyle.outlineWidth
        var y = panel.y + 5
        for (c in components) {
            c.x = x
            c.y = y
            c.w = cw
            c.h = componentHeight

            x += cw
            if (x >= panel.x + panel.w) {
                x = panel.x + panel.skin.panelStyle.outlineWidth
                y += componentHeight
            }
        }
    }
}