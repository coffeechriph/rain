package rain.api.gui.v2

import rain.api.Input

class TextField internal constructor(parent: Panel):
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value or GuiEventTypes.CHAR_EDIT.value) {
    var textEdited = false
    var string: String = ""
        set(value) {
            field = value
            parentPanel.compose = true
            cursorPos = string.length
        }

    private var cursorPos = 0
    private var displayStart = 0
    private var displayString = ""

    init {
        parentPanel = parent
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.textFieldStyle.textColor
        text.textAlign = skin.textFieldStyle.textAlign

        text.string = displayString

        var backColor = skin.textFieldStyle.backgroundColor
        if (active) {
            backColor = skin.textFieldStyle.activeColor
        }
        else if (hovered) {
            backColor = skin.textFieldStyle.hoverColor
        }

        val ow = skin.textFieldStyle.outlineWidth
        val back = when(skin.textFieldStyle.shape) {
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, backColor)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, 10.0f, backColor)
        }

        val cursor = if (active) {
            val vcursor = cursorPos - displayStart
            gfxCreateRect(x + parentPanel.font.getStringWidth(displayString, 0, vcursor), y, depth, 1.0f, h, skin.textFieldStyle.textColor)
        }
        else {
            floatArrayOf()
        }

        if (skin.textFieldStyle.outlineWidth > 0) {
            val outline = when(skin.textFieldStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.textFieldStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.textFieldStyle.outlineColor)
            }

            return cursor + back + outline
        }
        return cursor + back
    }

    override fun onCharEdit(input: Input) {
        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
            if (cursorPos > 0) {
                cursorPos -= 1
            }
        }

        if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
            if (cursorPos < string.length) {
                cursorPos += 1
            }
        }

        if (input.keyState(Input.Key.KEY_BACKSPACE) == Input.InputState.PRESSED) {
            if (cursorPos < string.length) {
                StringBuilder(string).deleteCharAt(cursorPos)
            }
            else {
                StringBuilder(string).substring(0, string.length-1)
            }
        }

        while (!input.isCodePointQueueEmpty()) {
            if (cursorPos < string.length) {
                StringBuilder(string).insert(cursorPos, input.popCodePointQueue())
            }
            else {
                StringBuilder(string).append(input.popCodePointQueue())
                cursorPos += 1
            }
        }
    }

    override fun onMouseEvent(input: Input) {
        parentPanel.compose = true
    }

    override fun handleState(): Boolean {
        textEdited = false
        return false
    }
}
