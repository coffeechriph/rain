package rain.api.gui

import org.joml.Vector2i
import org.joml.Vector4i
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTruetype.stbtt_GetCodepointKernAdvance
import org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.gfx.*
import rain.api.manager.addNewRenderComponentToRenderer
import rain.api.manager.removeRenderComponentFromRenderer
import rain.log
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer
import kotlin.math.floor

// TODO: We want a nice way to hide/show single components
// TODO: The problem currently is that if every component is hidden we can't update the vertex buffers as they
// TODO: Would be empty..
class Container(private val material: Material, private val textMaterial: Material, val resourceFactory: ResourceFactory, val font: Font) {
    val transform = Transform()
    var isDirty = false
    var visible = true
        set(value) {
            if (field != value) {
                isDirty = true
            }
            field = value

            if (::componentRenderComponent.isInitialized) {
                componentRenderComponent.visible = value
            }
            if (::textRenderComponent.isInitialized) {
                textRenderComponent.visible = value
            }
        }

    var background = false
        set(value) {
            if (field != value) {
                isDirty = true
            }
            field = value
        }
    var skin = Skin()
    private val components = ArrayList<GuiC>()
    private val textfields = ArrayList<Text>()
    private var lastTriggeredComponent: GuiC? = null
    private lateinit var componentBuffer: VertexBuffer
    private lateinit var textBuffer: VertexBuffer
    private lateinit var componentRenderComponent: RenderComponent
    private lateinit var textRenderComponent: RenderComponent

    private fun getUniformData(): ByteBuffer {
        val uniformData = memAlloc(18 * 4)
        val f = uniformData.asFloatBuffer()
        f.put(0, transform.x)
        f.put(1, transform.y)
        f.put(2, transform.sx)
        f.put(3, transform.sy)
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

    fun destroy() {
        if (::componentRenderComponent.isInitialized) {
            removeRenderComponentFromRenderer(componentRenderComponent)
        }

        if (::textRenderComponent.isInitialized) {
            removeRenderComponentFromRenderer(textRenderComponent)
        }
    }

    fun build() {
        isDirty = false

        buildComponentVertices()
        buildTextVertices()
    }

    private fun buildComponentVertices() {
        val depth = transform.z
        val list = ArrayList<Float>()

        for (component in components) {
            val x = component.x
            val y = component.y
            val w = component.w
            val h = component.h

            if (component is Button || component is ToggleButton) {
                val color = if (component.active) {
                    skin.activeColors["button"]
                } else {
                    skin.backgroundColors["button"]
                }!!

                if (!component.outline) {
                    list.addAll(listOf(
                            x, y, depth, color.x, color.y, color.z,
                            x, y + h, depth, color.x, color.y, color.z,
                            x + w, y + h, depth, color.x, color.y, color.z,
                            x + w, y + h, depth, color.x, color.y, color.z,
                            x + w, y, depth, color.x, color.y, color.z,
                            x, y, depth, color.x, color.y, color.z))
                }
                else {
                    val ow = component.outlineWidth / 2.0f
                    list.addAll(listOf(
                            x + ow, y + ow, depth, color.x, color.y, color.z,
                            x + ow, y + h - ow, depth, color.x, color.y, color.z,
                            x + w - ow, y + h - ow, depth, color.x, color.y, color.z,
                            x + w - ow, y + h - ow, depth, color.x, color.y, color.z,
                            x + w - ow, y + ow, depth, color.x, color.y, color.z,
                            x + ow, y + ow, depth, color.x, color.y, color.z))

                    val c = skin.borderColors["button"]!!
                    list.addAll(listOf(
                            x, y, depth-0.1f, c.x, c.y, c.z,
                            x, y + h, depth-0.1f, c.x, c.y, c.z,
                            x + w, y + h, depth-0.1f, c.x, c.y, c.z,
                            x + w, y + h, depth-0.1f, c.x, c.y, c.z,
                            x + w, y, depth-0.1f, c.x, c.y, c.z,
                            x, y, depth-0.1f, c.x, c.y, c.z))
                }
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
                val color = skin.backgroundColors["text"]!!
                list.addAll(listOf(
                        x, y, depth, color.x, color.y, color.z,
                        x, y + h, depth, color.x, color.y, color.z,
                        x + w, y + h, depth, color.x, color.y, color.z,
                        x + w, y + h, depth, color.x, color.y, color.z,
                        x + w, y, depth, color.x, color.y, color.z,
                        x, y, depth, color.x, color.y, color.z))
            }
        }

        // Add optional background for container
        if (background) {
            val w = transform.sx
            val h = transform.sy
            val color = skin.backgroundColors["container"]!!
            list.addAll(listOf(
                    0.0f, 0.0f, depth-0.15f, color.x, color.y, color.z,
                    0.0f, h, depth-0.15f, color.x, color.y, color.z,
                    w, h, depth-0.15f, color.x, color.y, color.z,
                    w, h, depth-0.15f, color.x, color.y, color.z,
                    w, 0.0f, depth-0.15f, color.x, color.y, color.z,
                    0.0f, 0.0f, depth-0.15f, color.x, color.y, color.z))
        }

        if (list.size > 0) {
            // TODO: Optimize this! We could put them directly
            val byteBuffer = memAlloc(list.size*4)
            val fbuffer = byteBuffer.asFloatBuffer()
            fbuffer.put(list.toFloatArray())
            fbuffer.flip()
            if (!::componentBuffer.isInitialized) {
                componentBuffer = resourceFactory.buildVertexBuffer()
                        .withVertices(byteBuffer)
                        .withState(VertexBufferState.DYNAMIC)
                        .withAttribute(VertexAttribute(0, 3))
                        .withAttribute(VertexAttribute(1, 3))
                        .build()

                val mesh = Mesh(componentBuffer, null)
                componentRenderComponent = RenderComponent(transform, mesh, material)
                componentRenderComponent.createUniformData = this::getUniformData
                componentRenderComponent.visible = visible
                addNewRenderComponentToRenderer(componentRenderComponent)
            }

            componentBuffer.update(byteBuffer)
        }
    }

    private fun buildTextVertices() {
        val depth = transform.z + 0.1f
        val scale = stbtt_ScaleForPixelHeight(font.fontInfo, font.fontHeight)
        val color = skin.foregroundColors["text"]!!

        var textSize = 0
        for (text in textfields) {
            textSize += text.string.length
        }

        var textVertexDataIndex = 0
        val textVertexData = FloatArray(textSize*8*6)

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
                val displayString = CharArray(text.string.length)
                var displayStringIndex = 0
                val paddingY = if (text.parent == null){ font.ascent*scale} else {0.0f}

                val vertList = FloatArray(text.string.length*8)
                var vertListIndex = 0
                while(index < text.string.length) {
                    if (x.get(0) >= boundsW * 0.9) {
                        break
                    }

                    displayString[displayStringIndex++] = text.string[index]
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

                        vertList[vertListIndex++] = quad.x0()
                        vertList[vertListIndex++] = quad.x1()
                        vertList[vertListIndex++] = quad.y0() + paddingY
                        vertList[vertListIndex++] = quad.y1() + paddingY
                        vertList[vertListIndex++] = quad.s0()
                        vertList[vertListIndex++] = quad.s1()
                        vertList[vertListIndex++] = quad.t0()
                        vertList[vertListIndex++] = quad.t1()
                    }
                }

                text.w = font.getStringWidth(String(displayString, 0, displayStringIndex), 0, displayStringIndex)
                text.h = font.fontHeight

                if (text.parent != null) {
                    text.x = text.parent.x + text.parent.w / 2 - text.w / 2
                    text.y = text.parent.y + text.parent.h / 2 + text.h / 4
                }

                for (i in 0 until vertListIndex/8) {
                    val cx1 = text.x + vertList[i*8]
                    val cx2 = text.x + vertList[i*8+1]
                    val cy1 = text.y + vertList[i*8+2]
                    val cy2 = text.y + vertList[i*8+3]
                    val ux1 = vertList[i*8+4]
                    val ux2 = vertList[i*8+5]
                    val uy1 = vertList[i*8+6]
                    val uy2 = vertList[i*8+7]

                    if (textVertexDataIndex >= textVertexData.size) {
                        log("Index out of bounds: $textVertexDataIndex, ${textVertexData.size}, $textSize")
                        break
                    }

                    textVertexData[textVertexDataIndex++] = cx1
                    textVertexData[textVertexDataIndex++] = cy1
                    textVertexData[textVertexDataIndex++] = depth
                    textVertexData[textVertexDataIndex++] = color.x
                    textVertexData[textVertexDataIndex++] = color.y
                    textVertexData[textVertexDataIndex++] = color.z
                    textVertexData[textVertexDataIndex++] = ux1
                    textVertexData[textVertexDataIndex++] = uy1

                    textVertexData[textVertexDataIndex++] = cx1
                    textVertexData[textVertexDataIndex++] = cy2
                    textVertexData[textVertexDataIndex++] = depth
                    textVertexData[textVertexDataIndex++] = color.x
                    textVertexData[textVertexDataIndex++] = color.y
                    textVertexData[textVertexDataIndex++] = color.z
                    textVertexData[textVertexDataIndex++] = ux1
                    textVertexData[textVertexDataIndex++] = uy2

                    textVertexData[textVertexDataIndex++] = cx2
                    textVertexData[textVertexDataIndex++] = cy2
                    textVertexData[textVertexDataIndex++] = depth
                    textVertexData[textVertexDataIndex++] = color.x
                    textVertexData[textVertexDataIndex++] = color.y
                    textVertexData[textVertexDataIndex++] = color.z
                    textVertexData[textVertexDataIndex++] = ux2
                    textVertexData[textVertexDataIndex++] = uy2

                    textVertexData[textVertexDataIndex++] = cx2
                    textVertexData[textVertexDataIndex++] = cy2
                    textVertexData[textVertexDataIndex++] = depth
                    textVertexData[textVertexDataIndex++] = color.x
                    textVertexData[textVertexDataIndex++] = color.y
                    textVertexData[textVertexDataIndex++] = color.z
                    textVertexData[textVertexDataIndex++] = ux2
                    textVertexData[textVertexDataIndex++] = uy2

                    textVertexData[textVertexDataIndex++] = cx2
                    textVertexData[textVertexDataIndex++] = cy1
                    textVertexData[textVertexDataIndex++] = depth
                    textVertexData[textVertexDataIndex++] = color.x
                    textVertexData[textVertexDataIndex++] = color.y
                    textVertexData[textVertexDataIndex++] = color.z
                    textVertexData[textVertexDataIndex++] = ux2
                    textVertexData[textVertexDataIndex++] = uy1

                    textVertexData[textVertexDataIndex++] = cx1
                    textVertexData[textVertexDataIndex++] = cy1
                    textVertexData[textVertexDataIndex++] = depth
                    textVertexData[textVertexDataIndex++] = color.x
                    textVertexData[textVertexDataIndex++] = color.y
                    textVertexData[textVertexDataIndex++] = color.z
                    textVertexData[textVertexDataIndex++] = ux1
                    textVertexData[textVertexDataIndex++] = uy1
                }
            }
        }

        if (textVertexDataIndex > 0) {
            // TODO: Optimize this, we could put it directly
            val byteBuffer = memAlloc(textVertexDataIndex*4)
            val fbuffer = byteBuffer.asFloatBuffer()
            fbuffer.put(textVertexData, 0, textVertexDataIndex)
            //fbuffer.flip()
            if (!::textBuffer.isInitialized) {
                textBuffer = resourceFactory.buildVertexBuffer()
                        .withVertices(byteBuffer)
                        .withState(VertexBufferState.DYNAMIC)
                        .withAttribute(VertexAttribute(0, 3))
                        .withAttribute(VertexAttribute(1, 3))
                        .withAttribute(VertexAttribute(2, 2))
                        .build()


                val mesh = Mesh(textBuffer, null)
                textRenderComponent = RenderComponent(transform, mesh, textMaterial)
                textRenderComponent.createUniformData = this::getUniformData
                textRenderComponent.visible = visible
                addNewRenderComponentToRenderer(textRenderComponent)
            }
            else {
                textBuffer.update(byteBuffer)
            }
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
        if (lastTriggeredComponent != null) {
            if (lastTriggeredComponent is Button) {
                updateButton(input)
            }
            else if (lastTriggeredComponent is ToggleButton) {
                updateToggleButton(input)
            }
        }

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
    }

    private fun updateButton(input: Input) {
        if (lastTriggeredComponent != null) {
            isDirty = lastTriggeredComponent!!.trigger()
            lastTriggeredComponent = null
        }
    }

    private fun updateToggleButton(input: Input) {

    }
}
