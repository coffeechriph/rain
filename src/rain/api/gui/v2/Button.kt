package rain.api.gui.v2

import rain.api.Input

class Button: Component(Input.InputState.PRESSED.value) {
    var clicked = false

    override fun createGraphic(skin: Skin): FloatArray {
        if (hovered || clicked) {
            return gfxCreateRect(x, y, 1.0f, w, h, skin.foreground.button)
        }

        return gfxCreateRect(x, y, 1.0f, w, h, skin.background.button)
    }

    override fun action(input: Input) {
        clicked = true
    }

    override fun handleState(): Boolean {
        if (clicked) {
            clicked = false
            return true
        }

        return false
    }
}