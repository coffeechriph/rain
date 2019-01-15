package rain.api.gfx

import rain.vulkan.VertexAttribute
import kotlin.AssertionError

typealias createVertexBufferHandler = (vertices: FloatArray, state: VertexBufferState, attributes: Array<VertexAttribute>) -> VertexBuffer
class VertexBufferBuilder internal constructor(val handler: createVertexBufferHandler) {
    private var vertices: FloatArray? = null
    private var state: VertexBufferState = VertexBufferState.STATIC
    private var attributes = ArrayList<VertexAttribute>()

    fun withVertices(vertices: FloatArray): VertexBufferBuilder {
        this.vertices = vertices
        return this
    }

    fun withState(state: VertexBufferState): VertexBufferBuilder {
        this.state = state
        return this
    }

    fun withAttribute(attribute: VertexAttribute): VertexBufferBuilder {
        attributes.add(attribute)
        return this
    }

    fun build(): VertexBuffer {
        if (vertices == null) {
            throw AssertionError("Unable to create vertex buffer without specifying vertices!")
        }
        if (attributes.isEmpty()) {
            throw AssertionError("No attributes specified for vertex buffer!")
        }

        val buffer = handler(vertices!!, state, attributes.toTypedArray())
        reset()
        return buffer
    }

    private fun reset() {
        vertices = null
        state = VertexBufferState.STATIC
        attributes.clear()
    }
}