package example.roguelike.Entity

import org.joml.Vector2i
import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Sprite
import rain.api.entity.Transform
import rain.api.scene.Scene

class XpBall(private val player: Player): Entity() {
    var cellX = 0
    var cellY = 0
    var pickedUp = false
    lateinit var transform: Transform
    lateinit var sprite: Sprite

    private var time = 0.0f
    private var beginPickup = false
    private var acc = 0.0000000000001

    // TODO: Constant window size
    fun setPosition(system: EntitySystem<XpBall>, pos: Vector2i) {
        val transform = system.findTransformComponent(getId())!!
        transform.x = pos.x.toFloat()
        transform.y = pos.y.toFloat()
        transform.z = 1.1f + transform.y * 0.001f
        transform.setScale(96.0f, 96.0f)
        cellX = pos.x / 1280
        cellY = pos.y / 768
    }

    override fun <T : Entity> init(scene: Scene, system: EntitySystem<T>) {
        transform = system.findTransformComponent(getId())!!
        sprite = system.findSpriteComponent(getId())!!
    }

    override fun <T : Entity> update(scene: Scene, input: Input, system: EntitySystem<T>, deltaTime: Float) {
        if (sprite.visible) {
            time += 1.0f / 60.0f
            transform.y += Math.sin(time.toDouble()).toFloat() * 0.1f

            if (!beginPickup && !pickedUp) {
                if (player.transform.x >= transform.x - 128 && player.transform.x <= transform.x + 128 &&
                        player.transform.y >= transform.y - 128 && player.transform.y <= transform.y + 128) {
                    beginPickup = true
                    acc = 0.0000000000001
                }
            }
            else {
                val dx = (player.transform.x - transform.x) / 64.0
                val dy = (player.transform.y - transform.y) / 64.0
                val ln = Math.sqrt((dx*dx+dy*dy))
                transform.x += ((dx / ln) * acc).toFloat()
                transform.y += ((dy / ln) * acc).toFloat()
                if (acc < 3.7f) {
                    acc += acc
                }

                if (player.transform.x >= transform.x - 8 && player.transform.x <= transform.x + 8 &&
                        player.transform.y >= transform.y - 8 && player.transform.y <= transform.y + 8) {
                    pickedUp = true
                    beginPickup = false
                    sprite.visible = false
                    player.addXp(5 * player.currentLevel)
                }
            }
        }
    }
}