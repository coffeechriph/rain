package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.entity.*
import rain.api.scene.Scene
import kotlin.random.Random

enum class ItemType {
    NONE,
    MELEE,
    RANGED,
    HEAD,
    SHOULDER,
    LEGS,
    CHEST,
    GLOVES,
    BOOTS,
    CONSUMABLE
}

val ITEM_NAMES = arrayOf("Iron Sword", "Steel Sword", "Stone Sword", "Iron Sword of Madness", "Iron Sword of Confusion",
                         "Bag of Coin")
class Item (val type: ItemType, val name: String, val stamina: Int, val strength: Int, val agility: Int, val luck: Int): Entity() {
    var cellX = 0
    var cellY = 0
    var pickedUp = false
    lateinit var transform: Transform
    lateinit var sprite: Sprite
    lateinit var collider: Collider

    // TODO: Constant window size
    fun setPosition(system: EntitySystem<Item>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.z = 1.1f + transform.y * 0.001f
        transform.setScale(96.0f, 96.0f)
        val body = system.findColliderComponent(getId())!!
        body.setPosition(pos.x.toFloat()%1280, pos.y.toFloat()%720)
        cellX = pos.x / 1280
        cellY = pos.y / 720
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        transform = system.findTransformComponent(getId())!!
        sprite = system.findSpriteComponent(getId())!!
        collider = system.findColliderComponent(getId())!!
    }

    // TODO: Problematic that we can't access the system from here....
    override fun onCollision(entity: Entity) {
        if (entity is Player) {
            pickedUp = true
            entity.inventory.addItem(this)
        }
    }
}
