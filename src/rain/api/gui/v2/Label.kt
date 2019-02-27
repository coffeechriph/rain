package rain.api.gui.v2

import rain.api.Input

class Label internal constructor(panel: Panel): Component(0) {
    var string: String = ""
        set(value) {
            field = value
            text.string = value
        }
    var background = false
        set(value) {
            field = value
            parentPanel.compose = true
        }

    init {
        parentPanel = panel
    }

    override fun createGraphic(depth: Float, skin: Skin): FloatArray {
        if (background) {
            val backColor = skin.labelStyle.backgroundColor
            return gfxCreateRect(x,y,depth,w,h,backColor)
        }

        text.color = skin.labelStyle.textColor
        text.textAlign = skin.labelStyle.textAlign
        return floatArrayOf()
    }

    override fun action(input: Input) {

    }

    override fun handleState(): Boolean {
        return false
    }

}
