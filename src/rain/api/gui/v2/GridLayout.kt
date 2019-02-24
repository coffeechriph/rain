package rain.api.gui.v2

class GridLayout : Layout() {
    var gridW = 50.0f
    var gridH = 50.0f
    override fun manage(panel: Panel, components: List<Component>) {
        var stepX = panel.x
        var stepY = panel.y + 5
        for (c in components) {
            c.x = stepX
            c.y = stepY
            c.w = gridW
            c.h = gridH

            stepX += gridW
            if (stepX + gridW > panel.x + panel.w) {
                stepX = panel.x
                stepY += gridH
            }
        }
    }
}