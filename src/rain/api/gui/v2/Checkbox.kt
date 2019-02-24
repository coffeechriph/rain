package rain.api.gui.v2

import rain.api.Input

class Checkbox internal constructor(panel: Panel): Component(Input.InputState.PRESSED.value) {
    var checked = false
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
        val backColor = skin.buttonStyle.backgroundColor
        val checkColor = if (checked) { skin.buttonStyle.activeColor } else { skin.buttonStyle.hoverColor }

        val ow = skin.buttonStyle.outlineWidth

        val check = when(skin.buttonStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow.toFloat(), y + ow.toFloat(), 1.0f, h - ow*2, h - ow*2, checkColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow.toFloat(), y + ow.toFloat(), 1.0f, h - ow*2, h - ow*2, 10.0f, checkColor)
        }

        val back = when(skin.buttonStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, 1.0f, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, 1.0f, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        if (skin.buttonStyle.outlineWidth > 0) {
            val outline = when(skin.buttonStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, 1.0f, w, h, skin.buttonStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, 1.0f, w, h, 10.0f, skin.buttonStyle.outlineColor)
            }

            return check + back + outline
        }

        return check + back
    }

    override fun action(input: Input) {
        if (input.mousePosition.x >= x && input.mousePosition.x < x + h) {
            checked = !checked
        }
    }

    override fun handleState(): Boolean {
        return false
    }

}