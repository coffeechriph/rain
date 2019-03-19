package rain.api.gui.v2

import rain.api.Input

class TextField internal constructor(parent: Panel):
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value or GuiEventTypes.CHAR_EDIT.value) {
    var textEdited = false
    var string: String = ""
        set(value) {
            field = value
            parentPanel.compose = true
            cursorPos = value.length
            internalString = value
        }
        get(){
            return internalString
        }
    private var internalString = ""
    private var cursorPos = 0
    private var displayStart = 0
    private var displayEnd = 0
    private var displayString = ""

    init {
        parentPanel = parent
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        updateDisplayString()
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

    private fun updateDisplayString() {
        displayStart = 0
        displayEnd = internalString.length
        displayString = ""

        while (parentPanel.font.getStringWidth(internalString, displayStart, displayEnd) > w) {
            if (displayStart < cursorPos) {
                displayStart++
            }

            if (displayEnd > cursorPos) {
                displayEnd--
            }
        }

        for (i in displayStart until displayEnd) {
            displayString += internalString[i]
        }
    }

    override fun onCharEdit(input: Input) {
        if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
            if (cursorPos > 0) {
                cursorPos -= 1
                parentPanel.compose = true
            }
        }

        if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
            if (cursorPos < internalString.length) {
                cursorPos += 1
                parentPanel.compose = true
            }
        }

        if (input.keyState(Input.Key.KEY_BACKSPACE) == Input.InputState.PRESSED) {
            if (internalString.isNotEmpty()) {
                if (cursorPos < internalString.length) {
                    internalString = StringBuilder(internalString).deleteCharAt(cursorPos).toString()
                } else {
                    internalString = StringBuilder(internalString).substring(0, internalString.length - 1)
                }

                if (cursorPos > 0) {
                    cursorPos--
                }

                parentPanel.compose = true
                textEdited = true
            }
        }

        while (!input.isCodePointQueueEmpty()) {
            val ch = input.popCodePointQueue().toChar()
            if (cursorPos < internalString.length) {
                internalString = StringBuilder(internalString).insert(cursorPos, ch).toString()
            }
            else {
                internalString = StringBuilder(internalString).append(ch).toString()
                cursorPos += 1
            }
            parentPanel.compose = true
            textEdited = true
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
