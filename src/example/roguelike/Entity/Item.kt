package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.entity.Entity
import rain.api.entity.EntitySystem

enum class ItemType {
    MELEE,
    RANGED,
    HEAD,
    SHOULDER,
    LEGS,
    CHEST,
    HANDS,
    CONSUMABLE
}

class Item (val type: ItemType): Entity() {
    var cellX = 0
    var cellY = 0
    var pickedUp = false

    // TODO: Constant window size
    fun setPosition(system: EntitySystem<Item>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.z = 2.1f + transform.y * 0.001f
        transform.setScale(96.0f, 96.0f)
        val body = system.findColliderComponent(getId())!!
        body.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%720)
        cellX = pos.x / 1280
        cellY = pos.y / 720
    }

    // TODO: Problematic that we can't access the system from here....
    override fun onCollision(entity: Entity) {
        if (entity is Player) {
            pickedUp = true
        }
    }
}
