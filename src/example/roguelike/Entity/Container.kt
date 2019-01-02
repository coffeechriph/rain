package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.*
import rain.api.scene.Scene

class Container(val containerType: Int) : Entity() {
    var health = 0
    var open = false
    var looted = false
    var cellX = 0
    var cellY = 0

    lateinit var transform: Transform
    lateinit var sprite: Sprite
    lateinit var collider: Collider

    // TODO: Constant window size
    fun setPosition(pos: Vector2i) {
        transform.z = 2.0f + transform.y * 0.001f
        transform.setScale(64.0f, 64.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 768
        collider.setPosition((pos.x%1280).toFloat(), (pos.y%768).toFloat())
        transform.x = collider.getPosition().x
        transform.y = collider.getPosition().y
    }

    override fun onCollision(entity: Entity) {
        if (!open && entity is Attack) {
            open = true
        }
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        transform = system.findTransformComponent(getId())!!
        sprite = system.findSpriteComponent(getId())!!
        collider = system.findColliderComponent(getId())!!
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (open) {
            sprite.textureTileOffset.x = 1
            sprite.textureTileOffset.y = containerType+4
        }
        else {
            sprite.textureTileOffset.x = 0
            sprite.textureTileOffset.y = containerType+4
        }
    }
}
