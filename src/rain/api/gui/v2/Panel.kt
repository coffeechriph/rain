package rain.api.gui.v2

import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.components.RenderComponent
import rain.api.entity.Entity
import rain.api.gfx.*
import rain.api.manager.renderManagerAddRenderComponent
import rain.vulkan.DataType
import rain.vulkan.VertexAttribute

class Panel internal constructor(var layout: Layout): Entity() {
    var x = 0.0f
    var y = 0.0f
    var w = 0.0f
    var h = 0.0f
    var skin = DEFAULT_SKIN
    var background = true
    var resizable = true
    var moveable = true
    internal var compose = true
    private val components = ArrayList<Component>()
    private lateinit var renderComponent: RenderComponent
    private lateinit var vertexBuffer: VertexBuffer
    private lateinit var mesh: Mesh

    fun addComponent(component: Component): Panel {
        component.parentPanel = this
        components.add(component)
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

        if (background) {
            vertices.addAll(gfxCreateRect(x, y, 0.0f, w, h, skin.background.panel).toTypedArray())
        }

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
}