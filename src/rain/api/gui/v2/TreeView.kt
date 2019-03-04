package rain.api.gui.v2

import org.joml.Vector4f
import rain.api.Input

class TreeView internal constructor(panel: Panel): Component(Input.InputState.PRESSED.value) {
    var selectedItem: ListItem? = null
    internal val items = ArrayList<ListItem>()
    private var currentItemY: Float = 0.0f

    init {
        parentPanel = panel
    }

    fun addItem(string: String): ListItem {
        val item = ListItem(this)
        item.string = string
        item.x = x
        item.y = y + (items.size*30)
        items.add(item)
        parentPanel.compose = true
        return item
    }

    private fun getItemAtPoint(x: Float, y: Float, item: ListItem): ListItem? {
        if (x >= item.x && x <= item.x + item.w &&
            y >= item.y && y <= item.y + item.h) {
            return item
        }

        if (item.expanded) {
            for (child in item.children) {
                val clicked = getItemAtPoint(x,y,child)
                if (clicked != null) {
                    return clicked
                }
            }
        }

        return null
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        return gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.backgroundColor)
    }

    override fun createTextGraphic(depth: Float, skin: Skin): FloatArray {
        currentItemY = 0.0f
        val itemHeight = parentPanel.font.fontHeight
        val list = ArrayList<Float>()
        for (item in items) {
            item.x = x
            item.y = currentItemY
            currentItemY += itemHeight
            list.addAll(gfxCreateText(item.x, item.y, depth, 100.0f, itemHeight, TextAlign.LEFT, item.string, parentPanel.font, skin.buttonStyle.textColor).toTypedArray())

            if (item.children.size > 0 && item.expanded) {
                list.addAll(createText(item, itemHeight, depth, skin.buttonStyle.textColor))
            }
        }

        return list.toFloatArray()
    }

    private fun createText(item: ListItem, itemHeight: Float, depth: Float, color: Vector4f): List<Float> {
        val list = ArrayList<Float>()
        for (i in item.children) {
            i.x = item.x + 10
            i.y = currentItemY
            currentItemY += itemHeight
            list.addAll(gfxCreateText(i.x, i.y, depth, 100.0f, itemHeight, TextAlign.LEFT, i.string, parentPanel.font, color).toTypedArray())

            if (i.children.size > 0 && i.expanded) {
                list.addAll(createText(i, itemHeight, depth, color))
            }
        }

        return list
    }

    override fun onClick(input: Input) {
        for (item in items) {
            val clicked = getItemAtPoint(input.mousePosition.x.toFloat(), input.mousePosition.y.toFloat(), item)
            if (clicked != null) {
                if (clicked.children.size > 0) {
                    clicked.expanded = !clicked.expanded
                }
                else {
                    selectedItem = clicked
                }
            }
        }
    }

    override fun handleState(): Boolean {
        selectedItem = null
        return false
    }

}
