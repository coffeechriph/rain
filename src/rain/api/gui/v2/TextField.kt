package rain.api.gui.v2

import rain.api.Input

class TextField internal constructor(parent: Panel):
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value or GuiEventTypes.CHAR_EDIT.value) {
    var textEdited = false
    var string: String = ""
        set(value) {
            field = value
            text.string = value
        }
    private var cursorPos = 0
    private var editString = ""

    init {
        parentPanel = parent
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
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

        if (skin.textFieldStyle.outlineWidth > 0) {
            val outline = when(skin.textFieldStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.textFieldStyle.outlineColor)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.textFieldStyle.outlineColor)
            }

            return back + outline
        }

        return back
    }

    override fun onCharEdit(input: Input) {
        // TODO: Ensure the text is always visible around the cursor
        if (active) {
            if (input.keyState(Input.Key.KEY_LEFT) == Input.InputState.PRESSED) {
                if (cursorPos > 0) {
                    cursorPos -= 1
                }
            }
            else if (input.keyState(Input.Key.KEY_RIGHT) == Input.InputState.PRESSED) {
                if (cursorPos < editString.length) {
                    cursorPos += 1
                }
            }
            else if (input.keyState(Input.Key.KEY_BACKSPACE) == Input.InputState.PRESSED) {
                if (editString.isNotEmpty() && cursorPos > 0) {
                    editString = StringBuilder(editString).deleteCharAt(cursorPos-1).toString()
                    cursorPos -= 1
                    textEdited = true
                }
            }

            var codepoint = input.popCodePointQueue()
            while(codepoint > -1) {
                editString = StringBuilder(editString).insert(cursorPos, codepoint.toChar()).toString()
                cursorPos += 1
                codepoint = input.popCodePointQueue()
                textEdited = true
            }

            string = editString
            text.string = StringBuilder(editString).insert(cursorPos, '|').toString()
        }
    }

    override fun handleState(): Boolean {
        textEdited = false
        return false
    }
}
