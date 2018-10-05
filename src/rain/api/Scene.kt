package rain.api

class Scene {
    val entitySystems = ArrayList<EntitySystem>()

    fun registerSystem(system: EntitySystem) {
        entitySystems.add(system)
    }

    internal fun update() {
        for (system in entitySystems) {
            for (update in system.updateIterator()) {
                update.update(this)
            }

            for (sprite in system.spriteIterator()) {
                val transform = system.findTransformComponent(sprite.entity)!!
                sprite.transform.position = transform.position
                sprite.transform.scale = transform.scale
                sprite.transform.rotation = transform.rotation
            }
        }
    }
}