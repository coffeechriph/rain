package rain.api.gui.v2

import rain.api.Input

class TreeView: Component(Input.InputState.PRESSED.value) {
    private val items = ArrayList<ListItem>()
    private val visibleItems = ArrayList<ListItem>()

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClick(input: Input) {
        for (item in items) {
            if (input.mousePosition.x >= item.x && input.mousePosition.x < item.x + item.w &&
                input.mousePosition.y >= item.y && input.mousePosition.y < item.y + item.h) {
                item.expanded = true
            }
        }

        visibleItems.clear()
        for (item in items) {
            if (item.expanded) {
                visibleItems.add(item)
                visibleItems.addAll(item.getVisibleChildren())
            }
        }
    }

    override fun handleState(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}