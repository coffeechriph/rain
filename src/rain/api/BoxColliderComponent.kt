package rain.api

class BoxColliderComponent(val entityId: Long, val tag: String, var x: Float, var y: Float, val w: Float, val h: Float) {
    var active = true

    fun collides(other: BoxColliderComponent): Boolean {
        return  other.x + other.w >= x && other.x <= x + w &&
                other.y + other.h >= y && other.y <= y + h
    }

    fun willCollide(plusX: Float, plusY: Float, other: BoxColliderComponent): Boolean {
        return  other.x + other.w < x + plusX || other.x > x + w + plusX ||
                other.y + other.h < y + plusY || other.y > y + h + plusY
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoxColliderComponent

        if (entityId != other.entityId) return false
        if (tag != other.tag) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (w != other.w) return false
        if (h != other.h) return false
        if (active != other.active) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entityId.hashCode()
        result = 31 * result + tag.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + w.hashCode()
        result = 31 * result + h.hashCode()
        result = 31 * result + active.hashCode()
        return result
    }
}