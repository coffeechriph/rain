package rain.api.gui.v2

class FillRowLayout: Layout() {
    var componentsPerRow = 1
    var componentHeight = 30.0f
    var rowPadding = 0.0f

    override fun manage(panel: Panel) {
        val cw = (panel.w - panel.skin.panelStyle.outlineWidth*2) / componentsPerRow
        var x = panel.x + panel.skin.panelStyle.outlineWidth
        var y = panel.y + 5
        var num = 0
        for (c in panel.components) {
            if (!c.visible) {
                continue
            }

            c.x = x
            c.y = y
            c.w = cw
            c.h = componentHeight

            x += cw
            num += 1
            if (num >= componentsPerRow) {
                num = 0
                x = panel.x + panel.skin.panelStyle.outlineWidth
                y += componentHeight + rowPadding
            }
        }
    }
}
