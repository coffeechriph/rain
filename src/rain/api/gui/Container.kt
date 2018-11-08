package rain.api.gui

import org.joml.Vector2i
import org.joml.Vector4i
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.entity.Transform
import rain.api.gfx.*
import java.nio.ByteBuffer

class Container(private val material: Material, val resourceFactory: ResourceFactory): Drawable() {
    val transform = Transform()
    var isDirty = true

    private val components = ArrayList<GuiC>()
    private var lastTriggeredComponent: GuiC? = null
    private lateinit var vertexBuffer: VertexBuffer

    override fun getTransform(): Transform {
        return transform
    }

    override fun getStreamedUniformData(): ByteBuffer {
        val uniformData = memAlloc(18 * 4)
        val f = uniformData.asFloatBuffer()
        f.put(transform.x)
        f.put(transform.y)
        f.put(transform.sx)
        f.put(transform.sy)
        f.put(floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
        f.flip()
        return uniformData
    }

    override fun getMaterial(): Material {
        return material
    }

    fun addComponent(component: GuiC) {
        components.add(component)
        isDirty = true
    }

    fun build() {
        isDirty = false

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
                        x, y, px, 0.0f,
                        x, y + h, px, 0.5f,
                        x + w, y + h, px + 0.25f, 0.5f,
                        x + w, y + h, px + 0.25f, 0.5f,
                        x + w, y, px + 0.25f, 0.0f,
                        x, y, px, 0.0f))
            }
        }

        if (!::vertexBuffer.isInitialized) {
            vertexBuffer = resourceFactory.createVertexBuffer(list.toFloatArray(), VertexBufferState.DYNAMIC)
        }

        vertexBuffer.update(list.toFloatArray())
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
            val mp = input.mousePosition
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
        renderer.submitDraw(this, vertexBuffer)
    }
}
