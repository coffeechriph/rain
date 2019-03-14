package rain.api.gui.v2

internal class TreeViewLayout: Layout() {
    var itemHeight = 24.0f
    private var itemY = 0.0f
    override fun apply(components: List<Component>, panelX: Float, panelY: Float, panelWidth:
    Float, panelHeight: Float, outlineWidth: Float) {
        val paddingY = itemHeight * 0.9f
        itemY = panelY
        for (item in components) {
            if (!item.visible) {
                continue
            }

            if (item is ListItem && item.parent != null) {
                item.x = item.parent.x + 10
            }
            else {
                item.x = panelX
            }

            item.y = itemY
            item.w = panelWidth - outlineWidth
            item.h = itemHeight
            itemY += paddingY
        }
    }
}
