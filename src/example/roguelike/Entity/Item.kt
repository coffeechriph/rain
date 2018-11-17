package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.entity.*
import rain.api.scene.Scene

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

val ITEM_COMBINATIONS = arrayOf(
    Pair(ItemType.MELEE, arrayOf(   "Iron Sword", "Steel Sword", "Wood Sword", "Bronze Sword", "Diamond Sword", "Mithril Sword",
                                    "Iron Dagger", "Steel Dagger", "Wood Dagger", "Bronze Dagger", "Diamond Dagger", "Mithril Dagger",
                                    "Iron Spear", "Steel Spear", "Wood Spear", "Bronze Spear", "Diamond Spear", "Mithril Spear",
                                    "Iron Mace", "Steel Mace", "Wood Mace", "Bronze Mace", "Diamond Mace", "Mithril Mace")),
    Pair(ItemType.RANGED, arrayOf(  "Bow", "Long Bow", "Crossbow")),
    Pair(ItemType.HEAD, arrayOf(    "Helmet", "Hood", "Mask", "Cap")),
    Pair(ItemType.CHEST, arrayOf(   "Chest Armor", "Bronze Breastplate", "Bone Cage", "Iron Breastplate", "Mithril Breastplate")),
    Pair(ItemType.LEGS, arrayOf(    "Leather Leggings", "Cloth Leggings", "Old Underwear", "Simple Leggings", "Leather Pants", "Cloth Pants")),
    Pair(ItemType.GLOVES, arrayOf(  "Leather Gloves", "Cloth Gloves", "Iron Gloves", "Bronze Gloves", "Mithril Gloves")),
    Pair(ItemType.BOOTS, arrayOf(   "Leather Boots", "Cloth Boots", "Sandals", "Iron Boots", "Mithril Boots", "Bronze Boots"))
)

val ITEM_QUALITIES = arrayOf(
    Pair(0..15, "[Poor]"),
    Pair(16..30, "[Common]"),
    Pair(31..74, "[Good]"),
    Pair(75..94, "[Rare]"),
    Pair(95..100, "[Epic]")
)
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
