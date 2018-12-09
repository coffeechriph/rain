package rain.api.gui

import org.joml.Vector3f

class Skin {
    val backgroundColors: HashMap<String, Vector3f> = hashMapOf(
        Pair("container", Vector3f(0.3f, 0.3f, 0.3f)),
        Pair("button", Vector3f(0.4f, 0.4f, 0.4f)),
        Pair("text", Vector3f(0.4f, 0.4f, 0.4f))
    )
    val foregroundColors: HashMap<String, Vector3f> = hashMapOf(
        Pair("text", Vector3f(1.0f, 1.0f, 1.0f))
    )
    val borderColors: HashMap<String, Vector3f> = hashMapOf(
        Pair("container", Vector3f(0.2f, 0.2f, 0.2f)),
        Pair("button", Vector3f(0.2f, 0.2f, 0.2f)),
        Pair("text", Vector3f(0.2f, 0.2f, 0.2f))
    )
    val activeColors: HashMap<String, Vector3f> = hashMapOf(
        Pair("button", Vector3f(0.5f, 0.5f, 0.5f))
    )
}
