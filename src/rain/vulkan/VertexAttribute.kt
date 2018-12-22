package rain.vulkan

enum class DataType {
    FLOAT,
    INT
}

data class VertexAttribute(val location: Int, val count: Int, val dataType: DataType = DataType.FLOAT)
