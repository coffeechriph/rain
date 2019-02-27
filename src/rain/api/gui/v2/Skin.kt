package rain.api.gui.v2

import org.joml.Vector4f

enum class Shape {
    RECT,
    ROUNDED_RECT
}

data class PanelStyle(
        var background: Boolean,
        var outlineWidth: Int,
        var backgroundColor: Vector4f,
        var outlineColor: Vector4f,
        var shape: Shape)

data class ButtonStyle(
        var outlineWidth: Int,
        var backgroundColor: Vector4f,
        var activeColor: Vector4f,
        var hoverColor: Vector4f,
        var outlineColor: Vector4f,
        var textColor: Vector4f,
        var textAlign: TextAlign,
        var shape: Shape)

data class SliderStyle(
        var outlineWidth: Int,
        var backgroundColor: Vector4f,
        var activeColor: Vector4f,
        var hoverColor: Vector4f,
        var outlineColor: Vector4f,
        var textColor: Vector4f,
        var cursorColor: Vector4f,
        var textAlign: TextAlign,
        var shape: Shape,
        var cursorShape: Shape)

data class TextFieldStyle(
        var outlineWidth: Int,
        var backgroundColor: Vector4f,
        var activeColor: Vector4f,
        var hoverColor: Vector4f,
        var outlineColor: Vector4f,
        var textColor: Vector4f,
        var textAlign: TextAlign,
        var shape: Shape)

data class LabelStyle(
        var outlineWidth: Int,
        var backgroundColor: Vector4f,
        var activeColor: Vector4f,
        var hoverColor: Vector4f,
        var outlineColor: Vector4f,
        var textColor: Vector4f,
        var textAlign: TextAlign)

// TODO: Extend skin class to also be in charge on component styles
// such as: outline & shape
data class Skin(
        var panelStyle: PanelStyle,
        var buttonStyle: ButtonStyle,
        var sliderStyle: SliderStyle,
        var textFieldStyle: TextFieldStyle,
        var labelStyle: LabelStyle)

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

val DEFAULT_TEXTFIELD_STYLE = TextFieldStyle(
        0,
        Vector4f(0.4f, 0.4f, 0.4f, 1.0f),
        Vector4f(0.6f, 0.6f, 0.6f, 1.0f),
        Vector4f(0.45f, 0.45f, 0.45f, 1.0f),
        Vector4f(0.0f, 0.0f, 0.0f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        TextAlign.CENTER,
        Shape.RECT)

val DEFAULT_LABEL_STYLE = LabelStyle(
        0,
        Vector4f(0.4f, 0.4f, 0.4f, 1.0f),
        Vector4f(0.6f, 0.6f, 0.6f, 1.0f),
        Vector4f(0.45f, 0.45f, 0.45f, 1.0f),
        Vector4f(0.0f, 0.0f, 0.0f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        TextAlign.LEFT)

val DEFAULT_SKIN = Skin(DEFAULT_PANEL_STYLE,
                        DEFAULT_BUTTON_STYLE,
                        DEFAULT_SLIDER_STYLE,
                        DEFAULT_TEXTFIELD_STYLE,
                        DEFAULT_LABEL_STYLE)
