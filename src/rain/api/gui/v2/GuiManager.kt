package rain.api.gui.v2

import org.joml.Vector2f
import rain.api.Input
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory

internal enum class GuiEventTypes(val value: Int) {
    CLICK(2),
    HOVER(4),
    DRAG(8),
    ACTIVATE(16),
    CHAR_EDIT(32);
}

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
    val p = Panel(layout, font)
    panels.add(p)
    return p
}

fun guiManagerCreateTreeView(): TreeView {
    val t = TreeView(font)
    panels.add(t)
    return t
}

internal fun guiManagerHandleInput(input: Input) {
    val mx = input.mousePosition.x
    val my = input.mousePosition.y

    // We don't want to update component events if we're currently draging
    if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT).value and Input.InputState.DOWN.value == 0) {
        if (lastHoveredComponent != null) {
            val lhc = lastHoveredComponent!!
            if (mx < lhc.x || mx > lhc.x + lhc.w ||
                my < lhc.y || my > lhc.y + lhc.h) {
                lhc.hovered = false
                lhc.hoverLeave = true
                lhc.parentPanel.compose = true
                lastHoveredComponent = null
            }
        }
    }

    for (panel in panels) {
        if (!panel.visible) {
            continue
        }

        panel.updateComponents()

        // Only check these events if a drag event isn't happening
        if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT).value and Input.InputState.DOWN.value == 0) {
            if (mx >= panel.x && mx <= panel.x + panel.w &&
                my >= panel.y && my <= panel.y + panel.h) {
                val c = panel.findComponentAtPoint(mx.toFloat(), my.toFloat())
                if (c != null) {
                    if (c != lastHoveredComponent) {
                        panel.compose = true
                    }

                    onMouseHovered(c)
                    if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.PRESSED
                    || input.mouseState(Input.Button.MOUSE_BUTTON_LEFT) == Input.InputState.RELEASED &&
                        c.eventTypes and GuiEventTypes.CLICK.value != 0) {
                        if (lastActiveComponent != null) {
                            lastActiveComponent!!.active = false
                            lastActiveComponent!!.deactivated = true
                        }

                        lastActiveComponent = c
                        lastActiveComponent!!.activated = true
                        lastActiveComponent!!.active = true
                        c.onMouseEvent(input)
                        panel.compose = true
                    }
                } else {
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
    }

    if (lastActiveComponent != null) {
        val component = lastActiveComponent!!
        if (component.eventTypes and GuiEventTypes.CHAR_EDIT.value != 0) {
            component.onCharEdit(input)
        }

        if (component.eventTypes and GuiEventTypes.DRAG.value != 0) {
            if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT).value and Input.InputState.DOWN.value != 0) {
                component.onDrag(input)
            }
            else if (input.mouseState(Input.Button.MOUSE_BUTTON_LEFT).value and (Input.InputState.RELEASED.value or Input.InputState.UP.value) != 0) {
                lastActiveComponent = null
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

private fun onMouseHovered(c: Component) {
    if (lastHoveredComponent != null) {
        lastHoveredComponent!!.hovered = false
        lastHoveredComponent!!.hoverLeave = true
    }

    c.hovered = true
    c.hoverEnter = true
    lastHoveredComponent = c
}

internal fun guiManagerHandleGfx(maxClipDepth: Float) {
    for (panel in panels) {
        if (panel.compose) {
            panel.composeGraphics(2.0f, uiMaterial, resourceFactory)
            panel.composeText(2.0f, font, textMaterial, resourceFactory)
            panel.compose = false
        }
    }
}
