package rain.api.components

import org.joml.Matrix4f

private var transformUniqueId: Long = 0
class Transform {
    var parentTransform: Transform? = null
    private val id: Long = transformUniqueId++
    private val hashCode = id.hashCode()

    internal var updated: Boolean = true
        get() {
            val t = field
            field = false
            return t
        }
        private set

    var x: Float = 0.0f
        set(value) {
            updated = true
            field = value
        }

    var y: Float = 0.0f
        set(value) {
            updated = true
            field = value
        }

    var z: Float = 0.0f
        set(value) {
            updated = true
            field = value
        }

    var sx: Float = 1.0f
        set(value) {
            updated = true
            field = value
        }

    var sy: Float = 1.0f
        set(value) {
            updated = true
            field = value
        }

    var rot: Float = 0.0f
        set(value) {
            updated = true
            field = value
        }

    fun setPosition(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun setScale(x: Float, y: Float) {
        this.sx = x
        this.sy = y
    }

    fun matrix(): Matrix4f {
        val modelMatrix = Matrix4f()
        modelMatrix.rotateZ(rot)
        modelMatrix.translate(x, y, z)
        modelMatrix.scale(sx, sy, 1.0f)

        if (parentTransform != null) {
            val parentModel = parentTransform!!.matrix()
            return parentModel.mul(modelMatrix)
        }

        return modelMatrix
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Transform

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return hashCode
    }


}
