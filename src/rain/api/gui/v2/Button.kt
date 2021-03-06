package rain.api.gui.v2

import rain.api.Input

class Button internal constructor(panel: Panel) :
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value) {
    var string: String = ""
        set(value) {
            field = value
            text.string = value
        }

    private var pressed = false

    init {
        parentPanel = panel
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.buttonStyle.textColor
        text.textAlign = skin.buttonStyle.textAlign

        var backColor = skin.buttonStyle.backgroundColor
        if (pressed) {
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

        if (ow > 0) {
            val outline = when(skin.buttonStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.buttonStyle.outlineColor)
            }

            return back + outline
        }
        return back
    }

    override fun onMouseEvent(input: Input) {
        if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.PRESSED) {
            pressed = true
        }
        else if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.RELEASED &&
         pressed) {
            pressed = false
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
