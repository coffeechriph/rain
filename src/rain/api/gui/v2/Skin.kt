package rain.api.gui.v2

import org.joml.Vector4f
import rain.api.gui.TextAlign

enum class Shape {
    RECT,
    ROUNDED_RECT
}

data class PanelStyle(
        val background: Boolean,
        val outlineWidth: Int,
        val backgroundColor: Vector4f,
        val outlineColor: Vector4f,
        val shape: Shape)

data class ButtonStyle(
        val outlineWidth: Int,
        val backgroundColor: Vector4f,
        val activeColor: Vector4f,
        val hoverColor: Vector4f,
        val outlineColor: Vector4f,
        val textColor: Vector4f,
        val textAlign: TextAlign,
        val shape: Shape)

data class SliderStyle(
        val outlineWidth: Int,
        val backgroundColor: Vector4f,
        val activeColor: Vector4f,
        val hoverColor: Vector4f,
        val outlineColor: Vector4f,
        val textColor: Vector4f,
        val cursorColor: Vector4f,
        val textAlign: TextAlign,
        val shape: Shape,
        val cursorShape: Shape)

// TODO: Extend skin class to also be in charge on component styles
// such as: outline & shape
data class Skin(
        val panelStyle: PanelStyle,
        val buttonStyle: ButtonStyle,
        val sliderStyle: SliderStyle)

val DEFAULT_PANEL_STYLE = PanelStyle(
        true,
        0,
        Vector4f(0.1f, 0.1f, 0.1f, 1.0f),
        Vector4f(0.2f, 0.0f, 0.0f, 1.0f),
        Shape.RECT)

val DEFAULT_BUTTON_STYLE = ButtonStyle(
        0,
        Vector4f(0.4f, 0.4f, 0.4f, 1.0f),
        Vector4f(0.6f, 0.6f, 0.6f, 1.0f),
        Vector4f(0.45f, 0.45f, 0.45f, 1.0f),
        Vector4f(0.0f, 0.0f, 0.0f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        TextAlign.CENTER,
        Shape.RECT)

val DEFAULT_SLIDER_STYLE = SliderStyle(
        0,
        Vector4f(0.4f, 0.4f, 0.4f, 1.0f),
        Vector4f(0.6f, 0.6f, 0.6f, 1.0f),
        Vector4f(0.45f, 0.45f, 0.45f, 1.0f),
        Vector4f(0.0f, 0.0f, 0.0f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        Vector4f(0.2f, 0.2f, 0.2f, 1.0f),
        TextAlign.CENTER,
        Shape.RECT,
        Shape.RECT)

val DEFAULT_SKIN = Skin(DEFAULT_PANEL_STYLE,
                        DEFAULT_BUTTON_STYLE,
                        DEFAULT_SLIDER_STYLE)