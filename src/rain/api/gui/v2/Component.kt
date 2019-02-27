package rain.api.gui.v2

import rain.api.Input

abstract class Component(internal val inputFilter: Int) {
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
    var active = false
    var hovered = false
    internal var text = Text()
    internal lateinit var parentPanel: Panel

    abstract fun createGraphic(depth: Float, skin: Skin): FloatArray
    abstract fun action(input: Input)
    abstract fun handleState(): Boolean
}
