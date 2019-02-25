package rain.api.gui.v2

import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.components.RenderComponent
import rain.api.entity.Entity
import rain.api.gfx.*
import rain.api.gui.Font
import rain.api.gui.TextAlign
import rain.api.manager.renderManagerAddRenderComponent
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute

class Panel internal constructor(var layout: Layout): Entity() {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
    var h = 0.0f
    var skin = DEFAULT_SKIN
    var resizable = true
    var moveable = true
    internal var compose = true
    private val components = ArrayList<Component>()
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

    fun addText(text: Text): Panel {
        text.parentPanel = this
        texts.add(text)
        compose = true
        return this
    }

    fun removeComponent(component: Component): Panel {
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

    internal fun composeGraphics(uiMaterial: Material, resourceFactory: ResourceFactory) {
        layout.manage(this, components)
        val vertices = ArrayList<Float>()

        if (skin.panelStyle.background) {
            val ow = skin.panelStyle.outlineWidth
            vertices.addAll(gfxCreateRect(x + ow, y + ow, 0.0f, w - ow*2, h - ow*2, skin.panelStyle.backgroundColor).toTypedArray())

            if (ow > 0) {
                vertices.addAll(gfxCreateRect(x, y, 0.0f, w, h, skin.panelStyle.outlineColor).toTypedArray())
            }
        }

        // TODO: These constant conversions between different types of collections are sloooow
        for (c in components) {
            vertices.addAll(c.createGraphic(skin).toTypedArray())
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
            renderManagerAddRenderComponent(getId(), renderComponent)
        }
        else {
            vertexBuffer.update(byteBuffer)
        }
    }

    internal fun composeText(font: Font, textMaterial: Material, resourceFactory: ResourceFactory) {
        val vertices = ArrayList<Float>()
        for (t in texts) {
            t.w = font.getStringWidth(t.string, 0, t.string.length)
            var cx = if (t.parentComponent != null) { t.parentComponent!!.x } else { 0.0f }
            var cy = if (t.parentComponent != null) { t.parentComponent!!.y } else { 0.0f }
            var cw = if (t.parentComponent != null) { t.parentComponent!!.w } else { 0.0f }
            var align = TextAlign.CENTER
            var color = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)

            if (t.parentComponent != null) {
                cx = t.parentComponent!!.x
                cy = t.parentComponent!!.y
                cw = t.parentComponent!!.w

                // TODO: Could find a more scalable way of finding out this information
                if (t.parentComponent is Button) {
                    align = t.parentPanel.skin.buttonStyle.textAlign
                    color = t.parentPanel.skin.buttonStyle.textColor
                }
                else if (t.parentComponent is Slider) {
                    align = t.parentPanel.skin.sliderStyle.textAlign
                    color = t.parentPanel.skin.sliderStyle.textColor
                }
            }

            // TODO: These constant conversions between different types of collections are sloooow
            vertices.addAll(gfxCreateText(cx + t.x, cy + t.y, cw, align, t.string, font, color).toTypedArray())
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
            renderManagerAddRenderComponent(getId(), textRenderComponent)
        }
        else {
            textRenderComponent.visible = true
            textVertexBuffer.update(byteBuffer)
        }
    }
}