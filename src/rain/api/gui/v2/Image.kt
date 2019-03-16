package rain.api.gui.v2

import org.joml.Vector2f
import rain.api.Input

class Image internal constructor(panel: Panel) :
        Component(GuiEventTypes.CLICK.value or GuiEventTypes.HOVER.value or GuiEventTypes.ACTIVATE.value) {
    var imageTileIndexX = 0
    var imageTileIndexY = 0
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
        var uvs = arrayOf(
                Vector2f(-1.0f, -1.0f),
                Vector2f(-1.0f, -1.0f),
                Vector2f(-1.0f, -1.0f),
                Vector2f(-1.0f, -1.0f))

        if (uiMaterial.getTexture2d().isNotEmpty()) {
            val texture = uiMaterial.getTexture2d()[0]
            val u1 = imageTileIndexX.toFloat() * texture.getTexCoordWidth()
            val v1 = imageTileIndexY.toFloat() * texture.getTexCoordHeight()

            val u2 = imageTileIndexX.toFloat() * texture.getTexCoordWidth() + texture.getTexCoordWidth()
            val v2 = imageTileIndexY.toFloat() * texture.getTexCoordHeight() + texture.getTexCoordHeight()

            uvs = arrayOf(
                    Vector2f(u1, v1),
                    Vector2f(u1, v2),
                    Vector2f(u2, v2),
                    Vector2f(u2, v1))
        }

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
            Shape.RECT -> gfxCreateRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, backColor, uvs)
            Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x + ow, y + ow, depth, w - ow*2, h - ow*2, 10.0f, backColor, uvs)
        }

        if (ow > 0) {
            val outline = when(skin.buttonStyle.shape) {
                Shape.RECT -> gfxCreateRect(x, y, depth, w, h, skin.buttonStyle.outlineColor, uvs)
                Shape.ROUNDED_RECT -> gfxCreateRoundedRect(x, y, depth, w, h, 10.0f, skin.buttonStyle.outlineColor, uvs)
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
