package rain.api.gui.v2

import rain.api.Input

abstract class Component(internal val inputFilter: Int) {
    var x = 0.0f
    var y = 0.0f
    var w = 32.0f
    var h = 32.0f
    var layer = 0.0f
    var active = false
    var hovered = false
    internal lateinit var parentPanel: Panel

    abstract fun createGraphic(skin: Skin): FloatArray
    abstract fun action(input: Input)
    abstract fun handleState(): Boolean
}