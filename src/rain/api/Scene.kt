package rain.api

class Scene {
    private val entitySystems = ArrayList<EntitySystem>()
    private val tilemaps = ArrayList<Tilemap>()

    fun registerSystem(system: EntitySystem) {
        entitySystems.add(system)
    }

    fun registerTilemap(tilemap: Tilemap) {
        tilemaps.add(tilemap)
    }

    internal fun update(renderer: Renderer, input: Input) {
        for (tilemap in tilemaps) {
            renderer.submitDrawTilemap(tilemap)
        }

        for (system in entitySystems) {
            for (update in system.updateIterator()) {
                update.update(this, input, system.findTransformComponent(update.entity)!!, system.findSpriteComponent(update.entity)!!)
            }

            for (sprite in system.spriteIterator()) {
                val transform = system.findTransformComponent(sprite.entity)!!
                sprite.transform.transform.position = transform.transform.position
                sprite.transform.transform.scale = transform.transform.scale
                sprite.transform.transform.rotation = transform.transform.rotation
                renderer.submitDrawSprite(sprite.transform, sprite.material, sprite.textureTileOffset)
            }
        }
    }
}
