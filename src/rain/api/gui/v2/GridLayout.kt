package rain.api.gui.v2

class GridLayout : Layout() {
    var gridW = 50.0f
    var gridH = 50.0f
    override fun apply(components: List<Component>, panelX: Float, panelY: Float, panelWidth:
    Float, panelHeight: Float, outlineWidth: Float) {
        var stepX = panelX + outlineWidth
        var stepY = panelY + outlineWidth
        for (c in components) {
            if (!c.visible) {
                continue
            }

            c.x = stepX
            c.y = stepY
            c.w = gridW
            c.h = gridH

            stepX += gridW
            if (stepX + gridW > panelX + panelWidth) {
                stepX = panelX + outlineWidth
                stepY += gridH
            }
        }
    }
}
