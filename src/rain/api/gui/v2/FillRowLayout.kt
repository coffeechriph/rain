package rain.api.gui.v2

class FillRowLayout: Layout() {
    var componentsPerRow = 1
    var componentHeight = 30.0f
    var rowPadding = 0.0f

    override fun manage(panel: Panel) {
        resetScrollbars(panel)
        updateFillableArea(panel)

        val scrollX = if (hScrollBar != null) { hScrollBar!!.value } else { 0.0f }
        val scrollY = if (vScrollBar != null) { vScrollBar!!.value } else { 0.0f }

        val cw = (panel.w - panel.skin.panelStyle.outlineWidth*2) / componentsPerRow
        var x = panel.x + panel.skin.panelStyle.outlineWidth
        var y = panel.y + 5
        var num = 0

        for (c in panel.components) {
            if (!c.visible) {
                continue
            }

            c.x = x - scrollX
            c.y = y - scrollY
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

        recreateScrollbars(panel, x, y)
    }
}
