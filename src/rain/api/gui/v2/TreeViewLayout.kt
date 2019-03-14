package rain.api.gui.v2

internal class TreeViewLayout: Layout() {
    var itemHeight = 24.0f
    private var itemY = 0.0f
    override fun manage(panel: Panel) {
        resetScrollbars(panel)
        updateFillableArea(panel)

        val scrollX = if (hScrollBar != null) { hScrollBar!!.value } else { 0.0f }
        val scrollY = if (vScrollBar != null) { vScrollBar!!.value } else { 0.0f }

        val treeView = panel as TreeView
        val paddingY = itemHeight * 0.9f
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        itemY = treeView.y + 5
        for (item in treeView.components) {
            if (!item.visible) {
                continue
            }

            if (item is ListItem && item.parent != null) {
                item.x = item.parent.x + 10 - scrollX
            }
            else {
                item.x = treeView.x + 5 - scrollX
            }

            item.y = itemY - scrollY
            item.w = fillableWidth
            item.h = itemHeight
            itemY += paddingY

            if (item.x > maxX) {
                maxX = item.x
            }
            if (item.y > maxY) {
                maxY = item.y
            }
        }

        recreateScrollbars(panel, maxX, maxY)
    }

    override fun updateFillableArea(panel: Panel) {
        val treeView = panel as TreeView
        val paddingY = itemHeight * 0.9f
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var itemX: Float
        itemY = treeView.y + 5
        for (item in treeView.components) {
            if (!item.visible) {
                continue
            }

            if (item is ListItem && item.parent != null) {
                itemX = item.parent.x + 10
            }
            else {
                itemX = treeView.x + 5
            }

            if (itemX > maxX) {
                maxX = itemX
            }
            if (itemY > maxY) {
                maxY = itemY
            }
            itemY += paddingY
        }

        if (maxX >= panel.w) {
            fillableWidth = panel.w - 30.0f
        }
        if (maxY >= panel.h) {
            fillableHeight = panel.h - 30.0f
        }
    }
}
