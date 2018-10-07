package rain.api

class Scene {
    private val entitySystems = ArrayList<EntitySystem>()

    fun registerSystem(system: EntitySystem) {
        entitySystems.add(system)
    }

    internal fun update(renderer: Renderer, input: Input) {
        for (system in entitySystems) {
            for (update in system.updateIterator()) {
                update.update(this, input, system.findTransformComponent(update.entity)!!, system.findSpriteComponent(update.entity)!!)
            }

            for (sprite in system.spriteIterator()) {
                val transform = system.findTransformComponent(sprite.entity)!!
                sprite.transform.position = transform.position
                sprite.transform.scale = transform.scale
                sprite.transform.rotation = transform.rotation
                renderer.submitDrawSprite(sprite.transform, sprite.material, sprite.textureTileOffset)
            }
        }
    }
}