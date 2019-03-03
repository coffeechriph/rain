package rain.api.gui.v2

internal class ListItem(val treeView: TreeView): Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value) {
    var string: String = "Item"
        set(value) {
            field = value
            text.string = value
        }
    var expanded = false
    val children = ArrayList<ListItem>()

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        return floatArrayOf()
    }

    override fun handleState(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getVisibleChildren(): List<ListItem> {
        val list = ArrayList<ListItem>()
        for (child in children) {
            if (child.expanded) {
                list.add(child)
                list.addAll(child.getVisibleChildren())
            }
        }

        return list
    }

}