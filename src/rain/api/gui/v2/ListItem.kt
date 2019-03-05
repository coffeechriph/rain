package rain.api.gui.v2

import rain.api.Input

class ListItem internal constructor(val parent: ListItem?, val treeView: TreeView): Component(GuiEventTypes.CLICK.value) {
    var string: String = "Item"
        set(value) {
            field = value
            text.string = value
        }
    var expanded = false
        set(value) {
            field = value
            for (child in children) {
                child.visible = value

                if (value && child.expanded) {
                    changeChildrenVisibility(child, true)
                }
                else {
                    changeChildrenVisibility(child, false)
                }
            }
        }

    private fun changeChildrenVisibility(child: ListItem, visible: Boolean) {
        for (item in child.children) {
            item.visible = visible

            if (visible && item.expanded) {
                changeChildrenVisibility(item, true)
            }
            else {
                changeChildrenVisibility(item, false)
            }
        }
    }

    internal val children = ArrayList<ListItem>()

    fun addItem(string: String): ListItem {
        val item = ListItem(this, treeView)
        item.parentPanel = treeView
        item.text.parentPanel = treeView
        item.text.parentComponent = item
        item.text.textAlign = TextAlign.LEFT
        item.string = string
        item.visible = expanded
        children.add(item)
        treeView.components.add(item)
        treeView.texts.add(item.text)
        return item
    }

    override fun onClick(input: Input) {
        if (children.size > 0) {
            expanded = !expanded
        }

        treeView.selectedItem = this
        treeView.compose = true
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        if (expanded) {
            return  gfxCreateRect(x - 1, y + h*0.5f, depth, 1.0f, h, skin.buttonStyle.outlineColor) +
                    gfxCreateRect(x - 1, y + h, depth, 10.0f, 1.0f, skin.buttonStyle.outlineColor)
        }
        return floatArrayOf()
    }

    override fun handleState(): Boolean {
        return false
    }
}
