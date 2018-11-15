package rain.api.gui

import org.joml.Vector2i
import org.joml.Vector4i
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.entity.Transform
import rain.api.gfx.*
import java.nio.ByteBuffer
import rain.vulkan.VertexAttribute
import kotlin.math.ceil
import kotlin.math.floor

class Container(private val material: Material, val resourceFactory: ResourceFactory, val font: Font) {
    val transform = Transform()
    var isDirty = false
    var visible = true
        set(value) {
            field = value
            isDirty = value
        }
    private val components = ArrayList<GuiC>()
    private val textfields = ArrayList<Text>()
    private var lastTriggeredComponent: GuiC? = null
    private lateinit var componentBuffer: VertexBuffer
    private lateinit var textBuffer: VertexBuffer

    private fun getUniformData(textureIndex: Int): ByteBuffer {
        val uniformData = memAlloc(18 * 4)
        val f = uniformData.asFloatBuffer()
        f.put(transform.x)
        f.put(transform.y)
        f.put(transform.sx)
        f.put(transform.sy)
        f.put(textureIndex.toFloat())
        f.put(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
        f.flip()

        return uniformData
    }

    fun addComponent(component: GuiC) {
        component.container = this
        components.add(component)

        val text = Text(component.text, component, this)
        text.w = font.getStringWidth(text.string, 0, text.string.length)
        text.h = font.fontHeight
        text.x = component.x + component.w/2 - text.w/2
        text.y = component.y + component.h/2 + text.h/4
        textfields.add(text)
        isDirty = true
    }

    fun removeComponent(component: GuiC) {
        components.remove(component)

        // TODO: Components could store their text object for faster removal
        for (text in textfields) {
            if (text.parent == component) {
                textfields.remove(text)
                break
            }
        }

        isDirty = true
    }

    fun addText(text: String, x: Float, y: Float, parent: GuiC? = null, background: Boolean = false): Text {
        val width = font.getStringWidth(text, 0, text.length)
        val height = font.fontHeight
        val txt = Text(text, parent, this)
        txt.x = x
        txt.y = y
        txt.w = width
        txt.h = height
        txt.background = background
        textfields.add(txt)
        isDirty = true
        return txt
    }

    fun removeText(text: Text) {
        textfields.remove(text)
        isDirty = true
    }

    fun build(renderer: Renderer) {
        isDirty = false

        buildComponentVertices(renderer)
        buildTextVertices(renderer)
    }

    private fun buildComponentVertices(renderer: Renderer) {
        val depth = renderer.getDepthRange().y - 0.1f
        val list = ArrayList<Float>()
        for (component in components) {
            val x = component.x
            val y = component.y
            val w = component.w
            val h = component.h

            if (component is Button || component is ToggleButton) {
                val px = if (component.active) {
                    0.5f
                } else {
                    0.0f
                }

                list.addAll(listOf(
                        x, y, depth, px, 0.0f,
                        x, y + h, depth, px, 0.5f,
                        x + w, y + h, depth, px + 0.25f, 0.5f,
                        x + w, y + h, depth, px + 0.25f, 0.5f,
                        x + w, y, depth, px + 0.25f, 0.0f,
                        x, y, depth, px, 0.0f))
            }
        }

        // Add optional background for text
        for (text in textfields) {
            if (text.background) {
                var w = floor(text.w)
                if (text.parent != null) {
                    text.string = text.parent.text
                    w = text.parent.w
                }

                val x = text.x
                val y = text.y
                val h = text.h
                // TODO: Support different styled backgrounds
                list.addAll(listOf(
                        x, y, depth, 0.0f, 0.0f,
                        x, y + h, depth, 0.0f, 0.5f,
                        x + w, y + h, depth, 0.25f, 0.5f,
                        x + w, y + h, depth, 0.25f, 0.5f,
                        x + w, y, depth, 0.25f, 0.0f,
                        x, y, depth, 0.0f, 0.0f))
            }
        }

        if (list.size > 0) {
            if (!::componentBuffer.isInitialized) {
                componentBuffer = resourceFactory.createVertexBuffer(list.toFloatArray(), VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 3), VertexAttribute(1, 2)))
            }

            componentBuffer.update(list.toFloatArray())
        }
    }

    private fun buildTextVertices(renderer: Renderer) {
        val depth = renderer.getDepthRange().y
        val scale = stbtt_ScaleForPixelHeight(font.fontInfo, font.fontHeight)

        val list = ArrayList<Float>()
        for (text in textfields) {
            var boundsW = Float.MAX_VALUE
            if (text.parent != null) {
                text.string = text.parent.text
                boundsW = text.parent.w
            }

            MemoryStack.stackPush().use { stack ->
                val codePoint = stack.mallocInt(1)
                val x = stack.floats(0.0f)
                val y = stack.floats(0.0f)
                val quad = STBTTAlignedQuad.mallocStack(stack)

                var index = 0
                var displayString = ""
                val paddingY = if (text.parent == null){ font.ascent*scale} else {0.0f}
                val vertList = ArrayList<Float>()
                while(index < text.string.length) {
                    if (x.get(0) >= boundsW * 0.9) {
                        break
                    }

                    displayString += text.string[index]
                    index += font.getCodePoint(text.string, text.string.length, index, codePoint)
                    val cp = codePoint.get(0)

                    if (cp == '\n'.toInt()) {
                        y.put(0, y.get(0) + (font.ascent - font.descent + font.lineGap) * scale)
                        x.put(0, 0.0f)
                    }
                    else {
                        font.getPackedQuad(cp, quad, x, y)
                        if (font.useKerning && index + 1 < text.string.length) {
                            font.getCodePoint(text.string, text.string.length, index, codePoint)
                            x.put(0, x.get(0) + stbtt_GetCodepointKernAdvance(font.fontInfo, cp, codePoint.get(0)) * scale)
                        }

                        vertList.add(quad.x0())
                        vertList.add(quad.x1())
                        vertList.add(quad.y0() + paddingY)
                        vertList.add(quad.y1() + paddingY)
                        vertList.add(quad.s0())
                        vertList.add(quad.s1())
                        vertList.add(quad.t0())
                        vertList.add(quad.t1())
                    }
                }

                text.w = font.getStringWidth(displayString, 0, displayString.length)
                text.h = font.fontHeight

                if (text.parent != null) {
                    text.x = text.parent.x + text.parent.w / 2 - text.w / 2
                    text.y = text.parent.y + text.parent.h / 2 + text.h / 4
                }

                for (i in 0 until vertList.size/8) {
                    val cx1 = text.x + vertList[i*8]
                    val cx2 = text.x + vertList[i*8+1]
                    val cy1 = text.y + vertList[i*8+2]
                    val cy2 = text.y + vertList[i*8+3]
                    val ux1 = vertList[i*8+4]
                    val ux2 = vertList[i*8+5]
                    val uy1 = vertList[i*8+6]
                    val uy2 = vertList[i*8+7]

                    list.addAll(listOf(
                            cx1, cy1, depth, ux1, uy1,
                            cx1, cy2, depth, ux1, uy2,
                            cx2, cy2, depth, ux2, uy2,
                            cx2, cy2, depth, ux2, uy2,
                            cx2, cy1, depth, ux2, uy1,
                            cx1, cy1, depth, ux1, uy1
                    ))
                }
            }
        }

        if (list.size > 0) {
            if (!::textBuffer.isInitialized) {
                textBuffer = resourceFactory.createVertexBuffer(list.toFloatArray(), VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 3), VertexAttribute(1, 2)))
            }

            textBuffer.update(list.toFloatArray())
        }
    }

    private fun isInside(pos: Vector2i, area: Vector4i): Boolean {
        if (pos.x < area.x || pos.x > area.x + area.z ||
            pos.y < area.y || pos.y > area.y + area.w) {
            return false
        }

        return true
    }

    // Handles interaction with gui elements. If a element is being interacted with the
    // input events will be de-registered in order to keep them from being forwarded to game code.
    fun update(input: Input) {
        val mouseDown = input.mouseState(Input.Button.MOUSE_BUTTON_1) == Input.InputState.PRESSED

        if (mouseDown) {
            val mp = Vector2i(input.mousePosition.x, input.mousePosition.y)
            if (isInside(mp, Vector4i(transform.x.toInt(), transform.y.toInt(), transform.sx.toInt(), transform.sy.toInt()))) {
                mp.x -= transform.x.toInt()
                mp.y -= transform.y.toInt()
                for (component in components) {
                    if (isInside(mp, Vector4i(component.x.toInt(), component.y.toInt(), component.w.toInt(), component.h.toInt()))) {
                        isDirty = component.trigger()
                        lastTriggeredComponent = component
                        break
                    }
                }
            }
        }

        if (lastTriggeredComponent != null) {
            if (lastTriggeredComponent is Button) {
                updateButton(input)
            }
            else if (lastTriggeredComponent is ToggleButton) (
                updateToggleButton(input)
            )
        }
    }

    private fun updateButton(input: Input) {
        if (lastTriggeredComponent != null) {
            val mp = input.mousePosition
            if (!isInside(mp, Vector4i(transform.x.toInt(), transform.y.toInt(), transform.sx.toInt(), transform.sy.toInt())) ||
                !isInside(Vector2i(mp.x - transform.x.toInt(), mp.y - transform.y.toInt()), Vector4i(lastTriggeredComponent!!.x.toInt(), lastTriggeredComponent!!.y.toInt(), lastTriggeredComponent!!.w.toInt(), lastTriggeredComponent!!.h.toInt()))) {
                isDirty = lastTriggeredComponent!!.trigger()
                lastTriggeredComponent = null
            }
            else if (input.mouseState(Input.Button.MOUSE_BUTTON_1) == Input.InputState.RELEASED) {
                isDirty = lastTriggeredComponent!!.trigger()
                lastTriggeredComponent = null
            }
        }
    }

    private fun updateToggleButton(input: Input) {

    }

    fun render(renderer: Renderer) {
        if (visible) {
            if (::componentBuffer.isInitialized) {
                renderer.submitDraw(Drawable(transform, material, getUniformData(0), componentBuffer))
            }

            if (::textBuffer.isInitialized) {
                renderer.submitDraw(Drawable(transform, material, getUniformData(1), textBuffer))
            }
        }
    }
}
