package rain.api.gui

import rain.api.Input

class Container {
    var x = 0.0f
    var y = 0.0f
    var sx = 0.0f
    var sy = 0.0f

    private val buttons = ArrayList<Button>()
    private val textFields = ArrayList<TextField>()
    private val toggleButtons = ArrayList<ToggleButton>()

    fun addButton(button: Button) {
        buttons.add(button)
    }

    fun addTextField(textField: TextField) {
        textFields.add(textField)
    }

    fun addToggleButtons(toggleButton: ToggleButton) {
        toggleButtons.add(toggleButton)
    }

    // Handles interaction with gui elements. If a element is being interacted with the
    // input events will be de-registered in order to keep them from being forwarded to game code.
    fun update(input: Input) {
        for (button in buttons) {
            button.active = false
        }

        if (input.mouseState(Input.Button.MOUSE_BUTTON_1) != Input.InputState.PRESSED) {
            return
        }

        val mp = input.mousePosition
        if (mp.x >= x && mp.x <= x + sx
        &&  mp.y >= y && mp.y <= y + sy) {
            for (button in buttons) {
                if (mp.x >= button.x && mp.x <= button.x + button.sx
                &&  mp.y >= button.y && mp.y <= button.y + button.sy) {
                    button.active = true
                    input.putMouseState(0, Input.InputState.UP)
                    return
                }
            }

            for (toggleButton in toggleButtons) {
                if (mp.x >= toggleButton.x && mp.x <= toggleButton.x + toggleButton.sx
                &&  mp.y >= toggleButton.y && mp.y <= toggleButton.y + toggleButton.sy) {
                    toggleButton.checked = !toggleButton.checked
                    input.putMouseState(0, Input.InputState.UP)
                    return
                }
            }

            for (textfield in textFields) {
                if (mp.x >= textfield.x && mp.x <= textfield.x + textfield.sx
                &&  mp.y >= textfield.y && mp.y <= textfield.y + textfield.sy) {
                    // TODO: Implement logic for adding whatever text is typed to this text field
                }
            }
        }
    }
}
