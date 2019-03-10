package rain.api.gui.v2

import rain.api.Input

class Checkbox internal constructor(panel: Panel):
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value) {
    var checked = false
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

        val backColor = skin.buttonStyle.backgroundColor
        val checkColor = if (checked) { skin.buttonStyle.activeColor } else { skin.buttonStyle.hoverColor }

        val ow = skin.buttonStyle.outlineWidth

        val check = when(skin.buttonStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow.toFloat(), y + ow.toFloat(), depth, h - ow*2, h - ow*2, checkColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow.toFloat(), y + ow.toFloat(), depth, h - ow*2, h - ow*2, 10.0f, checkColor)
        }

        val back = when(skin.buttonStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        if (skin.buttonStyle.outlineWidth > 0) {
            val outline = when(skin.buttonStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.buttonStyle.outlineColor)
            }

            return check + back + outline
        }
        return check + back
    }

    override fun onClick(input: Input) {
        if (input.mousePosition.x >= x && input.mousePosition.x < x + h) {
            checked = !checked
        }
    }

    override fun handleState(): Boolean {
        return false
    }
}
