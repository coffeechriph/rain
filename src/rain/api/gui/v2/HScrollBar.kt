package rain.api.gui.v2

import rain.api.Input

class HScrollBar internal constructor(panel: Panel) :
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.DRAG.value or GuiEventTypes.HOVER.value
                or
                GuiEventTypes.ACTIVATE.value) {
    var string: String = ""
        set(value) {
            field = value
            text.string = value
        }

    var maxScrollAmount = 100.0f
    var value = 0.0f
    var valueChanged = false

    init {
        parentPanel = panel
    }

    // TODO: Use scrollbar style
    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.sliderStyle.textColor
        text.textAlign = skin.sliderStyle.textAlign

        val factor = value / maxScrollAmount
        var backColor = skin.sliderStyle.backgroundColor
        val cursorColor = skin.sliderStyle.cursorColor
        val scrollW = w * factor

        if (hovered) {
            backColor = skin.sliderStyle.hoverColor
        }

        val ow = skin.sliderStyle.outlineWidth

        val cursor = when(skin.sliderStyle.cursorShape) {
            Shape.RECT -> gfxCreateRect(x + ow, y+ow, depth, scrollW, h - ow,
                    cursorColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y+ow, depth, scrollW, h-ow, 1.0f,
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
        val x = Math.max(0, Math.min(w.toInt(), input.mousePosition.x - x.toInt()))
        //val x = input.mousePosition.x - x
        val factor = 1.0f / w * x
        value = maxScrollAmount * factor
        value = Math.max(Math.min(maxScrollAmount, value), 0.0f)
        valueChanged = true
    }

    override fun handleState(): Boolean {
        return false
    }
}
