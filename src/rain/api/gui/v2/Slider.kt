package rain.api.gui.v2

import rain.api.Input

class Slider: Component(Input.InputState.PRESSED.value or Input.InputState.DOWN.value) {
    var valueChanged = false
    var value = 0
        set(value) {
            field = value
            parentPanel.compose = true
        }
    var minValue = 0
    var maxValue = 100

    override fun createGraphic(skin: Skin): FloatArray {
        val factor = value.toFloat() / (maxValue-minValue).toFloat()
        val cx = x + w * factor

        if (hovered) {
            val back = gfxCreateRect(x, y, 1.0f, w, h, skin.background.slider)
            val cursor = gfxCreateRect(cx, y, 2.0f, 2.0f, h, skin.foreground.slider)
            return back + cursor
        }

        val back = gfxCreateRect(x, y, 1.0f, w, h, skin.background.slider)
        val cursor = gfxCreateRect(cx, y, 2.0f, 2.0f, h, skin.foreground.slider)
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