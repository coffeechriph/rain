package rain.api

class Scene {
    private val entitySystems = ArrayList<EntitySystem<Entity>>()
    private val tilemaps = ArrayList<Tilemap>()
    private val cameras = ArrayList<Camera>()

    private var camera = Camera()

    fun<T: Entity> addSystem(system: EntitySystem<T>) {
        entitySystems.add(system as EntitySystem<Entity>)
    }

    fun addTilemap(tilemap: Tilemap) {
        tilemaps.add(tilemap)
    }

    fun addCamera(camera: Camera) {
        cameras.add(camera)
    }

    fun setActiveCamera(camera: Camera) {
        this.camera = camera
    }

    internal fun update(renderer: Renderer, input: Input, deltaTime: Float) {
        renderer.setActiveCamera(camera)

        for (tilemap in tilemaps) {
            renderer.submitDrawTilemap(tilemap)
        }

        for (system in entitySystems) {
            for (update in system.updateIterator()) {
                update.update(this, input, system)
            }

            for (sprite in system.spriteIterator()) {
                sprite.animationTime += deltaTime * 2.0f
                if (sprite.animationTime >= 1.0f) {
                    sprite.animationTime = 0.0f
                    sprite.animationIndex += 1

                    sprite.textureTileOffset.x = sprite.animation.startFrame + sprite.animationIndex

                    if (sprite.animationIndex >= (sprite.animation.endFrame - sprite.animation.startFrame)) {
                        sprite.animationIndex = 0
                    }
                }
                val transform = system.findTransformComponent(sprite.entity)!!
                sprite.transform.transform.position = transform.transform.position
                sprite.transform.transform.scale = transform.transform.scale
                sprite.transform.transform.rotation = transform.transform.rotation
                renderer.submitDrawSprite(sprite.transform, sprite.material, sprite.textureTileOffset)
            }
        }
    }
}
