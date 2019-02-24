package rain.api.gui.v2

import rain.api.Input

class Button internal constructor(panel: Panel): Component(Input.InputState.PRESSED.value) {
    var clicked = false
    var string: String = ""
        set(value) {
            field = value
            text.string = value
            parentPanel.compose = true
        }

    init {
        parentPanel = panel
    }

    override fun createGraphic(skin: Skin): FloatArray {
        var backColor = skin.buttonStyle.backgroundColor
        if (clicked) {
            backColor = skin.buttonStyle.activeColor
        }
        else if (hovered) {
            backColor = skin.buttonStyle.hoverColor
        }

        val ow = skin.buttonStyle.outlineWidth
        val back = when(skin.buttonStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, 1.0f, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, 1.0f, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        if (skin.buttonStyle.outlineWidth > 0) {
            val outline = when(skin.buttonStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, 1.0f, w, h, skin.buttonStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, 1.0f, w, h, 10.0f, skin.buttonStyle.outlineColor)
            }

            return back + outline
        }

        return back
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