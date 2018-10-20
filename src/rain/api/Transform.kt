package rain.api

class Transform {
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
}