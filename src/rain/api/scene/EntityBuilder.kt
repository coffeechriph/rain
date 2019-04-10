package rain.api.scene

import rain.api.components.Animator
import rain.api.components.RenderComponent
import rain.api.components.Transform
import rain.api.entity.Entity
import rain.api.gfx.Material
import rain.api.gfx.Mesh
import rain.api.manager.animatorManagerAddAnimatorComponent
import rain.api.manager.renderManagerAddRenderComponent

class EntityBuilder<T: Entity> internal constructor(private val scene: Scene, private val entity: T) {
    private var animator: Animator? = null
    private var renderComponent = ArrayList<RenderComponent>()

    fun attachAnimatorComponent(animator: Animator): EntityBuilder<T> {
        this.animator = animator
        return this
    }

    fun attachRenderComponent(material: Material, mesh: Mesh): EntityBuilder<T> {
        renderComponent.add(RenderComponent(Transform(), mesh, material))
        return this
    }

    fun build() {
        for (c in renderComponent) {
            c.transform = entity.transform
            renderManagerAddRenderComponent(entity.getId(), c)
        }

        if (animator != null) {
            animatorManagerAddAnimatorComponent(entity.getId(), animator!!)
            for (c in renderComponent) {
                c.textureTileOffset = animator!!.textureTileOffset
            }
        }

        entity.init(scene)
    }
}
