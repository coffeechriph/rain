package rain.api.gui.v2

// TODO: Add a generic type so we can decorate the list items with metadata
class TreeView internal constructor(font: Font): Panel(TreeViewLayout(), font) {
    var selectedItem: ListItem? = null
    private val items = ArrayList<ListItem>()

    fun addItem(string: String): ListItem {
        val item = ListItem(null, this)
        item.parentPanel = this
        item.text.parentComponent = item
        item.text.parentPanel = this
        item.text.textAlign = TextAlign.LEFT
        item.string = string
        components.add(item)
        texts.add(item.text)
        items.add(item)
        compose = true
        return item
    }

    fun removeItem(item: ListItem) {
        components.remove(item)
        texts.remove(item.text)
        items.remove(item)
        compose = true
    }

    fun items(): Iterator<ListItem> {
        return items.iterator()
    }

    fun clearItems() {
        for (item in items) {
            components.remove(item)
            texts.remove(item.text)
            clearSubItems(item)
        }

        items.clear()
    }

    private fun clearSubItems(item: ListItem) {
        for (child in item.children) {
            components.remove(child)
            texts.remove(child.text)
            clearSubItems(child)
        }
        item.children.clear()
    }
}
