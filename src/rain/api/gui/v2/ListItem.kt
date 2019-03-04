package rain.api.gui.v2

class ListItem internal constructor(val treeView: TreeView) {
    var string: String = "Item"
    var expanded = false
    internal var x = 0.0f
    internal var y = 0.0f
    internal var w = 100.0f
    internal var h = 20.0f
    internal val children = ArrayList<ListItem>()

    fun addItem(string: String): ListItem {
        val item = ListItem(treeView)
        item.x = x + 20
        item.y = y + 30
        item.string = string
        children.add(item)
        return item
    }
}
