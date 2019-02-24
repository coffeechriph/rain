package rain.api.gui.v2

import rain.api.Input
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gui.Font

private val panels = ArrayList<Panel>()
private lateinit var uiMaterial: Material
private lateinit var textMaterial: Material
private lateinit var font: Font
private lateinit var resourceFactory: ResourceFactory

private var lastActiveComponent: Component? = null
private var lastHoveredComponent: Component? = null
private var currentHookedPanel: Panel? = null
private var currentHookedMode = 0 // TODO: Add enum instead

internal fun guiManagerInit(resFactory: ResourceFactory) {
    resourceFactory = resFactory
    font = Font("./data/fonts/FreeSans.ttf")
    font.buildBitmap(resourceFactory, 1024, 1024, 20.0f)
    textMaterial = resourceFactory.buildMaterial()
            .withName("guiTextMaterial")
            .withVertexShader("./data/shaders/text.vert.spv")
            .withFragmentShader("./data/shaders/text.frag.spv")
            .withTexture(font.texture)
            .build()

    uiMaterial = resourceFactory.buildMaterial()
            .withName("uiMaterial")
            .withVertexShader("./data/shaders/guiv2.vert.spv")
            .withFragmentShader("./data/shaders/gui.frag.spv")
            .build()
}

fun guiManagerCreatePanel(layout: Layout): Panel {
    val p = Panel(layout)
    panels.add(p)
    return p
}

internal fun guiManagerHandleInput(input: Input) {
    val mx = input.mousePosition.x
    val my = input.mousePosition.y

    if (lastHoveredComponent != null) {
        val lhc = lastHoveredComponent!!
        if (mx < lhc.x || mx > lhc.x + lhc.w ||
            my < lhc.y || my > lhc.y + lhc.h) {
            lhc.hovered = false
            lhc.parentPanel.compose = true
            lastHoveredComponent = null
        }
    }

    for (panel in panels) {
        panel.updateComponents()
        if (mx >= panel.x && mx <= panel.x + panel.w &&
            my >= panel.y && my <= panel.y + panel.h) {
            val c = panel.findComponentAtPoint(mx.toFloat(), my.toFloat())
            if (c != null) {
                panel.compose = true

                onMouseHovered(c)
                if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT).value and c.inputFilter != 0) {
                    onMouseClicked(input, c)
                }
            }
            else {
                if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.PRESSED) {
                    // TODO: Add customizable coordinates as to where
                    // one must click in order to activate 'resize' or 'move' action
                    if (my >= panel.y + 5) {
                        if (panel.resizable) {
                            currentHookedPanel = panel
                            currentHookedMode = 0
                        }
                    } else {
                        if (panel.moveable) {
                            currentHookedPanel = panel
                            currentHookedMode = 1
                        }
                    }
                }
            }
        }
    }

    if (currentHookedPanel != null) {
        if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.RELEASED) {
            currentHookedPanel = null
        }
        else {
            val panel = currentHookedPanel!!
            if (currentHookedMode == 0) {
                panel.w = (input.mousePosition.x - panel.x)
                panel.h = (input.mousePosition.y - panel.y)
                panel.compose = true
            } else if (currentHookedMode == 1) {
                panel.x = input.mousePosition.x.toFloat()
                panel.y = input.mousePosition.y.toFloat()
                panel.compose = true
            }
        }
    }
}

private fun onMouseClicked(input: Input, c: Component) {
    if (lastActiveComponent != null) {
        lastActiveComponent!!.active = false
    }

    lastActiveComponent = c
    c.active = true
    c.action(input)
}

private fun onMouseHovered(c: Component) {
    if (lastHoveredComponent != null) {
        lastHoveredComponent!!.hovered = false
    }

    c.hovered = true
    lastHoveredComponent = c
}

internal fun guiManagerHandleGfx() {
    for (panel in panels) {
        if (panel.compose) {
            panel.compose = false
            panel.composeGraphics(uiMaterial, resourceFactory)
        }
    }
}