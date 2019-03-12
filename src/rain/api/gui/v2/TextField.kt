package rain.api.gui.v2

import rain.api.Input

class TextField internal constructor(parent: Panel):
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value or GuiEventTypes.CHAR_EDIT.value) {
    var textEdited = false
    var string: String = ""
        set(value) {
            field = value
            istring = value
            cursorPos = 0
            displayStart = 0
            displayEnd = value.length
            parentPanel.compose = true
        }

    private var istring: String = ""
    private var cursorPos = 0
    private var displayStart = 0
    private var displayEnd = 0
    private var displayString = ""

    init {
        parentPanel = parent
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        text.color = skin.textFieldStyle.textColor
        text.textAlign = skin.textFieldStyle.textAlign

        updateDisplayableString()
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
        clampCursorPos()

        // TODO: Ensure the text is always visible around the cursor
        if (active) {
            if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
                if (cursorPos > 0) {
                    cursorPos -= 1
                }
                // Always move display start back when we move cursor pos
                if (displayStart > 0) {
                    displayStart -= 1
                }
                if (parentPanel.font.getStringWidth(istring, displayStart, displayEnd) > w) {
                    displayEnd -= 1
                }
                parentPanel.compose = true
            }
            else if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
                if (cursorPos < istring.length) {
                    cursorPos += 1
                }

                // Move display start forward if the string gets too large
                if (parentPanel.font.getStringWidth(istring, displayStart, displayEnd) > w) {
                    displayStart += 1
                }
                if (displayEnd < istring.length) {
                    displayEnd += 1
                }
                parentPanel.compose = true
            }
            else if (input.keyState(Input.Key.KEY_BACKSPACE) == Input.InputState.PRESSED) {
                if (istring.isNotEmpty() && cursorPos > 0) {
                    istring = StringBuilder(istring).deleteCharAt(cursorPos - 1).toString()
                    cursorPos -= 1

                    // Always move display start back when we move cursor pos
                    if (displayStart > 0) {
                        displayStart -= 1
                    }
                    if (parentPanel.font.getStringWidth(istring, displayStart, displayEnd) > w) {
                        displayEnd -= 1
                    }
                    textEdited = true
                    parentPanel.compose = true
                }
            }

            var codepoint = input.popCodePointQueue()
            while(codepoint > -1) {
                istring = StringBuilder(istring).insert(cursorPos, codepoint.toChar()).toString()
                cursorPos += 1

                // Move display start forward if the string gets too large
                if (parentPanel.font.getStringWidth(istring, displayStart, cursorPos) > w) {
                    displayStart += 1
                }
                if (displayEnd < istring.length) {
                    displayEnd += 1
                }
                codepoint = input.popCodePointQueue()
                textEdited = true
                parentPanel.compose = true
            }
        }
    }

    override fun onMouseEvent(input: Input) {
        parentPanel.compose = true
    }

    private fun clampCursorPos() {
        if (cursorPos > istring.length) {
            cursorPos = istring.length
        } else if (cursorPos < 0) {
            cursorPos = 0
        }
    }

    private fun updateDisplayableString() {
        displayString = istring.substring(displayStart, displayEnd)
    }

    override fun handleState(): Boolean {
        textEdited = false
        return false
    }
}
