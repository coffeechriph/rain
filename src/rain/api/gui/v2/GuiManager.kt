package rain.api.gui.v2

import org.joml.Vector2f
import rain.api.Input
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory

private val panels = ArrayList<Panel>()
private lateinit var uiMaterial: Material
private lateinit var textMaterial: Material
private lateinit var font: Font
private lateinit var resourceFactory: ResourceFactory

private var lastActiveComponent: Component? = null
private var lastHoveredComponent: Component? = null
private var currentHookedPanel: Panel? = null
private var currentHookedMode = 0 // TODO: Add enum instead
private var lastHookedPoint = Vector2f(0.0f, 0.0f)

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

internal fun guiManagerClear() {
    panels.clear()
}

fun guiManagerCreatePanel(layout: Layout): Panel {
    val p = Panel(layout)
    panels.add(p)
    return p
}

/*
    TODO: THINGS TO IMPLEMENT
    TODO: 1) Register components to events and have methods for it. Click,Resize,Move,CharInput
    TODO: 2) Add generic triggers. onHoverEnter,onHoverLeave,onClick,onCharEdit,onResize,onMove,onActive,onDeactive
 */

internal fun guiManagerHandleInput(maxClipDepth: Float, input: Input) {
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

        // TODO: Special handling for component which use keyboard.
        // We should allow components to define which events they listen to and have methods for those cases
        if (lastActiveComponent != null && lastActiveComponent is TextField) {
            lastActiveComponent!!.action(input)
        }

        if (mx >= panel.x && mx <= panel.x + panel.w &&
            my >= panel.y && my <= panel.y + panel.h) {
            val c = panel.findComponentAtPoint(mx.toFloat(), my.toFloat())
            if (c != null) {
                panel.compose = true

                onMouseHovered(c)
                if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT).value and c.inputFilter != 0) {
                    onInput(input, c)
                }
            }
            else {
                if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.PRESSED) {
                    // TODO: Add customizable coordinates as to where
                    // one must click in order to activate 'resize' or 'move' action
                    if (mx >= panel.x + panel.w - 10 && my >= panel.y + panel.h - 10) {
                        if (panel.resizable) {
                            currentHookedPanel = panel
                            currentHookedMode = 0
                        }
                    } else {
                        if (panel.moveable) {
                            currentHookedPanel = panel
                            currentHookedMode = 1
                            lastHookedPoint = Vector2f(input.mousePosition.x.toFloat(), input.mousePosition.y.toFloat())
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
                val dx = input.mousePosition.x.toFloat() - lastHookedPoint.x
                val dy = input.mousePosition.y.toFloat() - lastHookedPoint.y

                panel.x += dx
                panel.y += dy

                lastHookedPoint.x = input.mousePosition.x.toFloat()
                lastHookedPoint.y = input.mousePosition.y.toFloat()

                panel.compose = true
            }
        }
    }
}

private fun onInput(input: Input, c: Component) {
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

internal fun guiManagerHandleGfx(maxClipDepth: Float) {
    for (panel in panels) {
        if (panel.compose) {
            panel.composeGraphics(maxClipDepth, uiMaterial, resourceFactory)
            panel.composeText(maxClipDepth, font, textMaterial, resourceFactory)
            panel.compose = false
        }
    }
}
