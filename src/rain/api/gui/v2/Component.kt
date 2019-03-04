package rain.api.gui.v2

import rain.api.Input

abstract class Component(internal val eventTypes: Int) {
    var x = 0.0f
        set(value) {
            field = value
            parentPanel.compose = true
        }
    var y = 0.0f
        set(value) {
            field = value
            parentPanel.compose = true
        }
    var w = 32.0f
        set(value) {
            field = value
            parentPanel.compose = true
        }
    var h = 32.0f
        set(value) {
            field = value
            parentPanel.compose = true

        }
    var layer = 0.0f
    var visible = true
        set(value) {
            field = value
            text.visible = value
        }
    var clicked = false
    var active = false
    var activated = false
    var deactivated = false
    var hovered = false
    var hoverEnter = false
    var hoverLeave = false
    var charEdited = false
    internal var text = Text()
    internal lateinit var parentPanel: Panel

    fun resetState() {
        hoverEnter = false
        hoverLeave = false
        activated = false
        deactivated = false
        charEdited = false
    }

    abstract fun createGraphic(depth: Float, skin: Skin): FloatArray
    open fun createTextGraphic(depth: Float, skin: Skin): FloatArray {
        return floatArrayOf()
    }

    // abstract fun action(input: Input)
    abstract fun handleState(): Boolean
    open fun onCharEdit(input: Input){}
    open fun onClick(input: Input){}
    open fun onDrag(input: Input){}
}
