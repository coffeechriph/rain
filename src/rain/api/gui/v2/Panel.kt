package rain.api.gui.v2

import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.components.RenderComponent
import rain.api.entity.Entity
import rain.api.gfx.*
import rain.api.manager.renderManagerAddRenderComponent
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute

private val PANEL_DEPTH = 0.3f
private val COMPONENT_DEPTH = 0.2f
private val TEXT_DEPTH = 0.1f

class Panel internal constructor(var layout: Layout): Entity() {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
    var h = 0.0f
    var skin = DEFAULT_SKIN
    var resizable = true
    var moveable = true
    var visible = true
        set(value) {
            field = value

            if (::renderComponent.isInitialized) {
                renderComponent.visible = value
                textRenderComponent.visible = value
            }
        }
    internal var compose = true
    internal val components = ArrayList<Component>()
    private val texts = ArrayList<Text>()
    private lateinit var renderComponent: RenderComponent
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var mesh: Mesh

    private lateinit var textRenderComponent: RenderComponent
    private lateinit var textVertexBuffer: VertexBuffer
    private lateinit var textMesh: Mesh

    fun createButton(string: String): Button {
        val button = Button(this)
        button.text.parentComponent = button
        button.text.parentPanel = this
        button.string = string
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
        components.add(button)
        texts.add(button.text)
        compose = true
        return button
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
        }
    }

    internal fun composeGraphics(maxClipDepth: Float, uiMaterial: Material, resourceFactory: ResourceFactory) {
        layout.manage(this)
        val vertices = ArrayList<Float>()

        if (skin.panelStyle.background) {
            val ow = skin.panelStyle.outlineWidth
            vertices.addAll(gfxCreateRect(x + ow, y + ow, maxClipDepth - PANEL_DEPTH, w - ow*2, h - ow*2, skin.panelStyle.backgroundColor).toTypedArray())

            if (ow > 0) {
                vertices.addAll(gfxCreateRect(x, y, maxClipDepth - PANEL_DEPTH, w, h, skin.panelStyle.outlineColor).toTypedArray())
            }
        }

        // TODO: These constant conversions between different types of collections are sloooow
        for (c in components) {
            vertices.addAll(c.createGraphic(maxClipDepth - COMPONENT_DEPTH, skin).toTypedArray())
        }

        val byteBuffer = memAlloc(vertices.size * 4)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (value in vertices) {
            floatBuffer.put(value)
        }
        floatBuffer.flip()

        if (!::renderComponent.isInitialized) {
            vertexBuffer = resourceFactory.buildVertexBuffer()
                    .withState(VertexBufferState.STATIC)
                    .withAttribute(VertexAttribute(0, 3))
                    .withAttribute(VertexAttribute(1, 3))
                    .withDataType(DataType.FLOAT)
                    .withVertices(byteBuffer)
                    .build()
            mesh = Mesh(vertexBuffer, null)
            renderComponent = RenderComponent(transform, mesh, uiMaterial)
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
            renderManagerAddRenderComponent(getId(), renderComponent)
        }
        else {
            vertexBuffer.update(byteBuffer)
        }
    }

    internal fun composeText(maxClipDepth: Float, font: Font, textMaterial: Material, resourceFactory: ResourceFactory) {
        val vertices = ArrayList<Float>()
        for (t in texts) {
            t.w = font.getStringWidth(t.string, 0, t.string.length)
            var cx = if (t.parentComponent != null) { t.parentComponent!!.x } else { 0.0f }
            var cy = if (t.parentComponent != null) { t.parentComponent!!.y } else { 0.0f }
            var cw = if (t.parentComponent != null) { t.parentComponent!!.w } else { 0.0f }

            if (t.parentComponent != null) {
                cx = t.parentComponent!!.x
                cy = t.parentComponent!!.y
                cw = t.parentComponent!!.w
            }

            // TODO: These constant conversions between different types of collections are sloooow
            vertices.addAll(gfxCreateText(cx + t.x, cy + t.y, maxClipDepth - TEXT_DEPTH, cw, t.textAlign, t.string, font, t.color).toTypedArray())
        }

        if (vertices.size <= 0) {
            if (::textRenderComponent.isInitialized) {
                textRenderComponent.visible = false
            }
            return
        }

        val byteBuffer = memAlloc(vertices.size * 4)
        val floatBuffer = byteBuffer.asFloatBuffer()
        for (value in vertices) {
            floatBuffer.put(value)
        }
        floatBuffer.flip()

        if (!::textRenderComponent.isInitialized) {
            textVertexBuffer = resourceFactory.buildVertexBuffer()
                    .withState(VertexBufferState.STATIC)
                    .withAttribute(VertexAttribute(0, 3))
                    .withAttribute(VertexAttribute(1, 3))
                    .withAttribute(VertexAttribute(2, 2))
                    .withDataType(DataType.FLOAT)
                    .withVertices(byteBuffer)
                    .build()
            textMesh = Mesh(textVertexBuffer, null)
            textRenderComponent = RenderComponent(transform, textMesh, textMaterial)
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
            renderManagerAddRenderComponent(getId(), textRenderComponent)
        }
        else {
            textVertexBuffer.update(byteBuffer)
        }
    }
}
