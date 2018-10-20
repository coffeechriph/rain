package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.Entity
import rain.api.EntitySystem
import rain.api.Input
import rain.api.Scene

open class Enemy : Entity() {
    lateinit var player: Player

    var cellX = 0
        private set
    var cellY = 0
        private set

    // TODO: Constant window size
    fun setPosition(system: EntitySystem<Enemy>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.position.x = pos.x.toFloat()%1280
        transform.position.y = pos.y.toFloat()%720
        transform.position.z = 2.0f + transform.position.y * 0.001f
        transform.scale.set(96.0f, 96.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 720
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {

    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {

    }
}
