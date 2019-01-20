package rain.api.gfx

import rain.vulkan.DataType
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

typealias createVertexBufferHandler = (vertices: ByteBuffer, state: VertexBufferState, attributes: Array<VertexAttribute>, dataType: DataType) -> VertexBuffer
class VertexBufferBuilder internal constructor(val handler: createVertexBufferHandler) {
    private var vertices: ByteBuffer? = null
    private var state: VertexBufferState = VertexBufferState.STATIC
    private var attributes = ArrayList<VertexAttribute>()
    private var dataType: DataType = DataType.FLOAT

    fun withDataType(dataType: DataType): VertexBufferBuilder {
        this.dataType = dataType
        return this
    }

    fun withVertices(vertices: ByteBuffer): VertexBufferBuilder {
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

        val buffer = handler(vertices!!, state, attributes.toTypedArray(), dataType)
        reset()
        return buffer
    }

    private fun reset() {
        vertices = null
        state = VertexBufferState.STATIC
        attributes.clear()
        dataType = DataType.FLOAT
    }
}