package rain.api.gui.v2

class FillRowLayout: Layout() {
    var componentsPerRow = 1
    var componentHeight = 30.0f
    var rowPadding = 0.0f

    override fun apply(components: List<Component>, panelX: Float, panelY: Float, panelWidth:
    Float, panelHeight: Float, outlineWidth: Float) {
        val cw = (panelWidth - outlineWidth*2) / componentsPerRow
        var x = panelX + outlineWidth
        var y = panelY
        var num = 0

        for (c in components) {
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
                x = panelX + outlineWidth
                y += componentHeight + rowPadding
            }
        }
    }
}
