package rain.api.gui.v2

import rain.api.Input

class VScrollBar internal constructor(panel: Panel) :
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.DRAG.value or GuiEventTypes.HOVER.value
                or GuiEventTypes.ACTIVATE.value) {
    var string: String = ""
        set(value) {
            field = value
            text.string = value
        }

    var maxScrollAmount = 100
    var value = 0
    var valueChanged = false

    init {
        parentPanel = panel
    }

    // TODO: Use scrollbar style
    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.sliderStyle.textColor
        text.textAlign = skin.sliderStyle.textAlign

        val factor = value.toFloat() / maxScrollAmount.toFloat()
        var backColor = skin.sliderStyle.backgroundColor
        val cursorColor = skin.sliderStyle.cursorColor
        val scrollH = h * factor

        if (hovered) {
            backColor = skin.sliderStyle.hoverColor
        }

        val ow = skin.sliderStyle.outlineWidth

        val cursor = when(skin.sliderStyle.cursorShape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow, scrollH, cursorColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow, scrollH, 1.0f,
                    cursorColor)
        }

        val back = when(skin.sliderStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        if (ow > 0) {
            val outline = when(skin.sliderStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.sliderStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.sliderStyle.outlineColor)
            }

            return cursor + back + outline
        }
        return cursor + back
    }

    override fun onDrag(input: Input) {
        val cy = Math.max(0, Math.min(h.toInt(), input.mousePosition.y - y.toInt()))
        val factor = 1.0f / h * cy
        value = (maxScrollAmount * factor).toInt()
        value = Math.max(Math.min(maxScrollAmount, value), 0)
        valueChanged = true
    }

    override fun handleState(): Boolean {
        return false
    }
}
