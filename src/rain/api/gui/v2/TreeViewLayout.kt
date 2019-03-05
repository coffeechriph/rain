package rain.api.gui.v2

internal class TreeViewLayout: Layout() {
    var itemHeight = 24.0f
    private var itemY = 0.0f
    override fun manage(panel: Panel) {

        val treeView = panel as TreeView
        val paddingY = itemHeight * 0.9f
        itemY = treeView.y + 5
        for (item in treeView.components) {
            if (!item.visible) {
                continue
            }

            if (item is ListItem && item.parent != null) {
                item.x = item.parent.x + 10
            }
            else {
                item.x = treeView.x + 5
            }

            item.y = itemY
            item.w = panel.w
            item.h = itemHeight
            itemY += paddingY
        }
    }
}
