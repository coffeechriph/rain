package rain.api.gui.v2

import rain.api.Input

class ToggleButton internal constructor(panel: Panel): Component(Input.InputState.PRESSED.value) {
    var checked = false
        set(value) {
            field = value
            parentPanel.compose = true
        }
    var string: String = ""
        set(value) {
            field = value
            text.string = value
        }

    init {
        parentPanel = panel
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.buttonStyle.textColor
        text.textAlign = skin.buttonStyle.textAlign

        var backColor = skin.buttonStyle.backgroundColor
        if (checked) {
            backColor = skin.buttonStyle.activeColor
        }
        else if (hovered) {
            backColor = skin.buttonStyle.hoverColor
        }

        val ow = skin.buttonStyle.outlineWidth
        val back = when(skin.buttonStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        if (skin.buttonStyle.outlineWidth > 0) {
            val outline = when(skin.buttonStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, 1.0f, w, h, skin.buttonStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.buttonStyle.outlineColor)
            }

            return back + outline
        }

        text.color = skin.buttonStyle.textColor
        text.textAlign = skin.buttonStyle.textAlign
        return back
    }

    override fun onMouseEvent(input: Input) {
        if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.RELEASED) {
            checked = true
            clicked = true
        }
    }

    override fun handleState(): Boolean {
        if (clicked) {
            clicked = false
            return true
        }

        return false
    }
}
