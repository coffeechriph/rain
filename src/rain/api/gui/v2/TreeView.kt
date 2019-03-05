package rain.api.gui.v2

class TreeView internal constructor(font: Font): Panel(TreeViewLayout(), font) {
    var selectedItem: ListItem? = null

    fun addItem(string: String): ListItem {
        val item = ListItem(null, this)
        item.parentPanel = this
        item.text.parentComponent = item
        item.text.parentPanel = this
        item.text.textAlign = TextAlign.LEFT
        item.string = string
        components.add(item)
        texts.add(item.text)
        compose = true
        return item
    }
}
