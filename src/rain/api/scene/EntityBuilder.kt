package rain.api.scene

import rain.api.components.Animator
import rain.api.components.MoveComponent
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.manager.animatorManagerAddAnimatorComponent
import rain.api.manager.moveManagerAddMoveComponent
import rain.api.manager.renderManagerAddRenderComponent

class EntityBuilder<T: Entity> internal constructor(private val scene: Scene, private var entityId: Long, private val entity: T) {
    private var animator: Animator? = null
    private var renderComponent = ArrayList<RenderComponent>()
    private var moveComponent: MoveComponent? = null

    fun attachAnimatorComponent(animator: Animator): EntityBuilder<T> {
        this.animator = animator
        return this
    }

    fun attachMoveComponent(vx: Float, vy: Float): EntityBuilder<T> {
        moveComponent = MoveComponent(0.0f, 0.0f, vx, vy, entityId)
        return this
    }

    fun attachRenderComponent(material: Material, mesh: Mesh): EntityBuilder<T> {
        renderComponent.add(RenderComponent(Transform(), mesh, material))
        return this
    }

    fun build() {
        for (c in renderComponent) {
            c.transform = entity.transform
            renderManagerAddRenderComponent(entityId, c)
        }

        if (moveComponent != null) {
            moveManagerAddMoveComponent(entityId, entity.transform, moveComponent!!.vx, moveComponent!!.vy)
        }

        if (animator != null) {
            animatorManagerAddAnimatorComponent(entityId, animator!!)
            for (c in renderComponent) {
                c.textureTileOffset = animator!!.textureTileOffset
            }
        }

        entity.init(scene)
    }
}
