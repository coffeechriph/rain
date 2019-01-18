package rain.api.entity

class Move(val transform: Transform, val vx: Float, val vy: Float) {
    fun update() {
        transform.x += vx
        transform.y += vy
    }
}
