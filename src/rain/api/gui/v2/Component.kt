package rain.api.gui.v2

import rain.api.Input

open class Component(internal val inputFilter: Int) {
    var x = 0.0f
    var y = 0.0f
    var w = 32.0f
    var h = 32.0f
    var layer = 0.0f
    var active = false
    var hovered = false
    internal lateinit var parentPanel: Panel

    open fun createGraphic(skin: Skin): FloatArray {
        if (hovered || active) {
            return gfxCreateRect(x, y, 0.0f, w, h, skin.foreground.button)
        }

        return gfxCreateRect(x, y, 0.0f, w, h, skin.background.button)
    }

    open fun action(input: Input) {}
    open fun handleState(): Boolean {
        return false
    }
}