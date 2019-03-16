package rain.api.gui.v2

import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.components.GuiRenderComponent
import rain.api.entity.Entity
import rain.api.gfx.*
import rain.api.manager.renderManagerAddGuiRenderComponent
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute

private val PANEL_DEPTH = 0.2f
private val COMPONENT_DEPTH = 0.3f
private val TEXT_DEPTH = 0.4f

// TODO: Make it possible for panels to follow other panels relative to criterias
// 1) Same Width, Height, X or Y. Panels that follow will also inherit the other panels Z
// Panels that follow should never overlap!
open class Panel internal constructor(var layout: Layout, var font: Font): Entity() {
    var x = 0.0f
        set(value) {
            field = value
            compose = true
        }
    var y = 0.0f
        set(value) {
            field = value
            compose = true
        }
    var z = 0.0f
        internal set(value) {
            field = value
            compose = true
        }
    var w = 0.0f
        set(value) {
            field = value
            compose = true
        }
    var h = 0.0f
        set(value) {
            field = value
            compose = true
        }
    var skin = DEFAULT_SKIN
    var resizable = true
    var moveable = true
    var visible = true
        set(value) {
            field = value
            if (::renderComponent.isInitialized) {
                renderComponent.visible = value
            }

            if (::textRenderComponent.isInitialized) {
                textRenderComponent.visible = value
            }
        }
    var autoScroll = true

    internal var compose = true
    internal val components = ArrayList<Component>()
    internal val texts = ArrayList<Text>()
    private lateinit var renderComponent: GuiRenderComponent
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var mesh: Mesh

    private lateinit var textRenderComponent: GuiRenderComponent
    private lateinit var textVertexBuffer: VertexBuffer
    private lateinit var textMesh: Mesh

    fun createButton(string: String): Button {
        val button = Button(this)
        button.text.parentComponent = button
        button.text.parentPanel = this
        button.string = string
        button.text.w = font.getStringWidth(string, 0, string.length)
        components.add(button)
        texts.add(button.text)
        compose = true
        return button
    }

    fun createSlider(value: Int, minValue: Int, maxValue: Int): Slider {
        val slider = Slider(this)
        slider.text.parentComponent = slider
        slider.text.parentPanel = this
        slider.value = value
        slider.minValue = minValue
        slider.maxValue = maxValue
        slider.text.w = font.getStringWidth(value.toString(), 0, value.toString().length)
        components.add(slider)
        texts.add(slider.text)
        compose = true
        return slider
    }

    fun createCheckbox(string: String): Checkbox {
        val checkbox = Checkbox(this)
        checkbox.text.parentComponent = checkbox
        checkbox.text.parentPanel = this
        checkbox.string = string
        checkbox.text.w = font.getStringWidth(string, 0, string.length)
        components.add(checkbox)
        texts.add(checkbox.text)
        compose = true
        return checkbox
    }

    fun createTextField(string: String): TextField {
        val textField = TextField(this)
        textField.text.parentComponent = textField
        textField.text.parentPanel = this
        textField.string = string
        textField.text.w = font.getStringWidth(string, 0, string.length)
        components.add(textField)
        texts.add(textField.text)
        compose = true
        return textField
    }

    fun createLabel(string: String): Label {
        val label = Label(this)
        label.text.parentComponent = label
        label.text.parentPanel = this
        label.string = string
        label.text.w = font.getStringWidth(string, 0, string.length)
        components.add(label)
        texts.add(label.text)
        compose = true
        return label
    }

    fun createToggleButton(string: String): ToggleButton {
        val button = ToggleButton(this)
        button.text.parentComponent = button
        button.text.parentPanel = this
        button.string = string
        button.text.w = font.getStringWidth(string, 0, string.length)
        components.add(button)
        texts.add(button.text)
        compose = true
        return button
    }

    fun createHScrollBar(maxScrollAmount: Int, string: String): HScrollBar {
        val scrollBar = HScrollBar(this)
        scrollBar.text.parentComponent = scrollBar
        scrollBar.text.parentPanel = this
        scrollBar.string = string
        scrollBar.text.w = font.getStringWidth(string, 0, string.length)
        components.add(scrollBar)
        texts.add(scrollBar.text)
        compose = true
        return scrollBar
    }

    fun createVScrollBar(maxScrollAmount: Int, string: String): VScrollBar {
        val scrollBar = VScrollBar(this)
        scrollBar.text.parentComponent = scrollBar
        scrollBar.text.parentPanel = this
        scrollBar.string = string
        scrollBar.text.w = font.getStringWidth(string, 0, string.length)
        components.add(scrollBar)
        texts.add(scrollBar.text)
        compose = true
        return scrollBar
    }

    fun createImage(imageTileIndexX: Int, imageTileIndexY: Int, string: String): Image {
        val image = Image(this)
        image.text.parentComponent = image
        image.text.parentPanel = this
        image.string = string
        image.text.w = font.getStringWidth(string, 0, string.length)
        image.imageTileIndexX = imageTileIndexX
        image.imageTileIndexY = imageTileIndexY
        components.add(image)
        texts.add(image.text)
        compose = true
        return image
    }

    fun removeComponent(component: Component): Panel {
        texts.remove(component.text)
        components.remove(component)
        return this
    }

    fun findComponentAtPoint(x: Float, y: Float): Component? {
        for (c in components) {
            if (x >= c.x && x < c.x + c.w &&
                y >= c.y && y < c.y + c.h) {
                return c
            }
        }
        return null
    }

    internal fun updateComponents() {
        for (c in components) {
            if(c.handleState()) {
                compose = true
            }
            c.resetState()
        }
    }

    internal fun composeGraphics(zOrder: Float, uiMaterial: Material, resourceFactory: ResourceFactory) {
        layout.manage(this)
        val vertices = ArrayList<Float>()

        if (skin.panelStyle.background) {
            val ow = skin.panelStyle.outlineWidth
            vertices.addAll(gfxCreateRect(x + ow, y + ow, zOrder + PANEL_DEPTH, w - ow*2, h - ow*2, skin.panelStyle.backgroundColor).toTypedArray())

            if (ow > 0) {
                vertices.addAll(gfxCreateRect(x, y, zOrder + PANEL_DEPTH, w, h, skin.panelStyle.outlineColor).toTypedArray())
            }
        }

        // TODO: These constant conversions between different types of collections are sloooow
        for (c in components) {
            if (c.visible) {
                vertices.addAll(c.createGraphic(zOrder + COMPONENT_DEPTH, skin).toTypedArray())
            }
        }

        val byteBuffer = memAlloc(vertices.size * 4)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (value in vertices) {
            floatBuffer.put(value)
        }
        floatBuffer.flip()

        if (!::renderComponent.isInitialized) {
            if (vertices.size > 0) {
                vertexBuffer = resourceFactory.buildVertexBuffer()
                        .withState(VertexBufferState.STATIC)
                        .withAttribute(VertexAttribute(0, 3))
                        .withAttribute(VertexAttribute(1, 3))
                        .withAttribute(VertexAttribute(2, 2))
                        .withDataType(DataType.FLOAT)
                        .withVertices(byteBuffer)
                        .build()
                mesh = Mesh(vertexBuffer, null)
                renderComponent = GuiRenderComponent(mesh, uiMaterial)
                renderComponent.createUniformData = {
                    val uniformData = memAlloc(18 * 4)
                    val f = uniformData.asFloatBuffer()
                    f.put(0, x)
                    f.put(1, y)
                    f.put(2, w)
                    f.put(3, h)
                    f.flip()

                    uniformData
                }
                renderComponent.visible = visible
                renderManagerAddGuiRenderComponent(renderComponent)
            }
        }
        else {
            if (vertices.size > 0) {
                vertexBuffer.update(byteBuffer)
                renderComponent.visible = visible
            }
            else {
                renderComponent.visible = false
            }
        }
    }

    internal fun composeText(zOrder: Float, font: Font, textMaterial: Material, resourceFactory: ResourceFactory) {
        val vertices = ArrayList<Float>()
        for (t in texts) {
            if (!t.visible) {
                continue
            }

            t.w = font.getStringWidth(t.string, 0, t.string.length)
            var cx = if (t.parentComponent != null) { t.parentComponent!!.x } else { 0.0f }
            var cy = if (t.parentComponent != null) { t.parentComponent!!.y } else { 0.0f }
            var cw = if (t.parentComponent != null) { t.parentComponent!!.w } else { 0.0f }
            var ch = if (t.parentComponent != null) { t.parentComponent!!.h } else { 0.0f }

            if (t.parentComponent != null) {
                cx = t.parentComponent!!.x
                cy = t.parentComponent!!.y
                cw = t.parentComponent!!.w
            }

            // TODO: These constant conversions between different types of collections are sloooow
            vertices.addAll(gfxCreateText(cx + t.x, cy + t.y, zOrder + TEXT_DEPTH, cw, ch, t.textAlign, t.string, font, t.color).toTypedArray())
        }

        val byteBuffer = memAlloc(vertices.size * 4)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (value in vertices) {
            floatBuffer.put(value)
        }
        floatBuffer.flip()

        if (!::textRenderComponent.isInitialized) {
            if (vertices.size > 0) {
                textVertexBuffer = resourceFactory.buildVertexBuffer()
                        .withState(VertexBufferState.STATIC)
                        .withAttribute(VertexAttribute(0, 3))
                        .withAttribute(VertexAttribute(1, 3))
                        .withAttribute(VertexAttribute(2, 2))
                        .withDataType(DataType.FLOAT)
                        .withVertices(byteBuffer)
                        .build()
                textMesh = Mesh(textVertexBuffer, null)
                textRenderComponent = GuiRenderComponent(textMesh, textMaterial)
                textRenderComponent.createUniformData = {
                    val uniformData = memAlloc(18 * 4)
                    val f = uniformData.asFloatBuffer()
                    f.put(0, x)
                    f.put(1, y)
                    f.put(2, w)
                    f.put(3, h)
                    f.flip()

                    uniformData
                }
                textRenderComponent.visible = visible
                renderManagerAddGuiRenderComponent(textRenderComponent)
            }
        }
        else {
            if (vertices.size > 0) {
                textVertexBuffer.update(byteBuffer)
                textRenderComponent.visible = visible
            }
            else {
                textRenderComponent.visible = false
            }
        }
    }
}
