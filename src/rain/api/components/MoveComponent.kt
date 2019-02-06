package rain.api.components

import rain.api.manager.moveManagerChangeMoveComponent

data class MoveComponent(val x: Float, val y: Float, val vx: Float, val vy: Float, private val id: Long) {
    fun update(vx: Float, vy: Float) {
        moveManagerChangeMoveComponent(id, vx, vy)
    }
}