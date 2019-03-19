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

    // TODO: Implement a remove method
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

    override fun onMouseEvent(input: Input) {
        if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.RELEASED) {
            if (children.size > 0) {
                expanded = !expanded
            }

            treeView.selectedItem = this
            treeView.compose = true
        }
    }

    // TODO: Make TreeView or ListItem skinnable
    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.buttonStyle.textColor
        text.textAlign = TextAlign.LEFT

        val body = if (treeView.selectedItem == this) {
            gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.backgroundColor)
        }
        else {
            if (hovered) {
                gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.hoverColor)
            }
            else {
                gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.activeColor)
            }
        }

        if (children.size > 0) {
            if (!expanded) {
                text.string = "+$string"
            }
            else {
                text.string = "-$string"
            }
        }

        return body
    }

    override fun handleState(): Boolean {
        return false
    }
}
