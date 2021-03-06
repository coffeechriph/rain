package rain.api.entity

import rain.api.Input
import rain.api.components.Animator
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.manager.animatorManagerGetAnimatorByEntity
import rain.api.manager.renderManagerGetRenderComponentByEntity
import rain.api.scene.Scene

private var currentId: Long = 0
private fun getNextUniqueId(): Long {
    if (currentId + 1 >= Long.MAX_VALUE) {
        throw AssertionError("Creating this entity would cause the ID to overflow!")
    }
    currentId += 1

    return currentId
}

open class Entity {
    private val id = getNextUniqueId()
    val transform = Transform()

    fun getId(): Long {
        return id
    }

    fun getRenderComponents(): List<RenderComponent> {
        return renderManagerGetRenderComponentByEntity(id) ?: emptyList()
    }

    fun getAnimatorComponent(): List<Animator> {
        return animatorManagerGetAnimatorByEntity(id) ?: emptyList()
    }

    open fun init(scene: Scene){}
    open fun update(scene: Scene, input: Input){}
    open fun onCollision(entity: Entity) {}
}
