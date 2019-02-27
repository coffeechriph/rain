package rain.api.gui.v2

import rain.api.Input

class Slider internal constructor(panel: Panel): Component(Input.InputState.PRESSED.value or Input.InputState.DOWN.value) {
    var valueChanged = false
    var value = 0
        set(value) {
            field = value
            parentPanel.compose = true
            text.string = value.toString()
        }
    var minValue = 0
    var maxValue = 100

    init {
        parentPanel = panel
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        val factor = value.toFloat() / (maxValue-minValue).toFloat()
        val cx = x + w * factor
        var backColor = skin.sliderStyle.backgroundColor
        val cursorColor = skin.sliderStyle.cursorColor

        if (hovered) {
            backColor = skin.sliderStyle.hoverColor
        }

        val ow = skin.sliderStyle.outlineWidth
        val back = when(skin.sliderStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        val cursor = when(skin.sliderStyle.cursorShape) {
            Shape.RECT -> gfxCreateRect(cx, y, depth, 2.0f, h, cursorColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(cx, y, depth, 2.0f, h, 1.0f, cursorColor)
        }

        if (ow > 0) {
            val outline = when(skin.sliderStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.sliderStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.sliderStyle.outlineColor)
            }

            return back + outline + cursor
        }

        text.color = skin.sliderStyle.textColor
        text.textAlign = skin.sliderStyle.textAlign
        return back + cursor
    }

    override fun action(input: Input) {
        val x = input.mousePosition.x - x
        val factor = 1.0f / w * x
        value = ((maxValue - minValue) * factor).toInt()
        valueChanged = true
    }

    override fun handleState(): Boolean {
        if (valueChanged) {
            valueChanged = false
        }
        return false
    }
}
