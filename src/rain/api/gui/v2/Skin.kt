package rain.api.gui.v2

import org.joml.Vector3f
import org.joml.Vector4f

enum class Shape {
    RECT,
    ROUNDED_RECT
}

fun rgb2float(r: Int, g: Int, b: Int): Vector3f {
    return Vector3f(r.toFloat() / 255.0f, g.toFloat() / 255.0f, b.toFloat() / 255.0f)
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
        Vector4f(0.094f, 0.196f, 0.318f, 1.0f),
        Shape.RECT)

val DEFAULT_BUTTON_STYLE = ButtonStyle(
        0,
        Vector4f(0.094f, 0.196f, 0.318f, 1.0f),
        Vector4f(0.154f, 0.256f, 0.378f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        TextAlign.CENTER,
        Shape.RECT)

val DEFAULT_SLIDER_STYLE = SliderStyle(
        0,
        Vector4f(0.094f, 0.196f, 0.318f, 1.0f),
        Vector4f(0.154f, 0.256f, 0.378f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        Vector4f(0.2f, 0.2f, 0.2f, 1.0f),
        TextAlign.CENTER,
        Shape.RECT,
        Shape.RECT)

val DEFAULT_TEXTFIELD_STYLE = TextFieldStyle(
        0,
        Vector4f(0.094f, 0.196f, 0.318f, 1.0f),
        Vector4f(0.154f, 0.256f, 0.378f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        TextAlign.CENTER,
        Shape.RECT)

val DEFAULT_LABEL_STYLE = LabelStyle(
        0,
        Vector4f(0.094f, 0.196f, 0.318f, 1.0f),
        Vector4f(0.154f, 0.256f, 0.378f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(0.194f, 0.296f, 0.418f, 1.0f),
        Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
        TextAlign.LEFT)

val DEFAULT_SKIN = Skin(DEFAULT_PANEL_STYLE,
                        DEFAULT_BUTTON_STYLE,
                        DEFAULT_SLIDER_STYLE,
                        DEFAULT_TEXTFIELD_STYLE,
                        DEFAULT_LABEL_STYLE)
