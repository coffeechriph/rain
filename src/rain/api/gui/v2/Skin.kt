package rain.api.gui.v2

import org.joml.Vector4f

data class Colors(
        val panel: Vector4f,
        val button: Vector4f,
        val textField: Vector4f,
        val checkBox: Vector4f,
        val slider: Vector4f,
        val text: Vector4f)

// TODO: Extend skin class to also be in charge on component styles
// such as: outline & shape
data class Skin(
        val background: Colors,
        val foreground: Colors,
        val text: Colors,
        val outline: Colors)

val DEFAULT_SKIN = Skin(
        Colors(Vector4f(0.4f, 0.4f, 0.4f, 1.0f),
               Vector4f(0.5f, 0.5f, 0.5f, 1.0f),
               Vector4f(0.5f, 0.5f, 0.5f, 1.0f),
               Vector4f(0.5f, 0.5f, 0.5f, 1.0f),
               Vector4f(0.5f, 0.5f, 0.5f, 1.0f),
               Vector4f(0.0f, 0.0f, 0.0f, 0.0f)),

        Colors(Vector4f(0.6f, 0.6f, 0.6f, 1.0f),
               Vector4f(0.7f, 0.7f, 0.7f, 1.0f),
               Vector4f(0.7f, 0.7f, 0.7f, 1.0f),
               Vector4f(0.7f, 0.7f, 0.7f, 1.0f),
               Vector4f(0.7f, 0.7f, 0.7f, 1.0f),
               Vector4f(0.0f, 0.0f, 0.0f, 0.0f)),

        Colors(Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f)),

        Colors(Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f),
               Vector4f(1.0f, 1.0f, 1.0f, 1.0f)))