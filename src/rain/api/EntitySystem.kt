package rain.api

import kotlin.IllegalStateException

/*
    EntitySystem keeps track of components for a specific entity type.

    Idea for nice creation:
        var system = renderer.newSystem<Entity>()
        val entity = system.newInstance()
                           .attachTransform()
                           .attachUpdate()
                           .attachSprite()
                           .create() -> Will throw if one component is missing a dependency
                           For example if there's no Transform attached but Sprite has been
                           attached.
 */
class EntitySystem {
    private var entityId: Long = 0
    private var entities = ArrayList<Long>()
    private var transformComponents = ArrayList<TransformComponent>()
    private var updateComponents = ArrayList<UpdateComponent>()
    private var spriteComponents = ArrayList<SpriteComponent>()

    fun newEntity(): Builder {
        entities.add(uniqueId())
        return Builder(this)
    }

    class Builder internal constructor(private var system: EntitySystem) {
        fun attachTransformComponent(): Builder {
            val c = TransformComponent(system.entities[system.entities.size - 1])
            system.transformComponents.add(c)
            return this
        }

        fun attachUpdateComponent(update: (scene: Scene) -> Unit): Builder {
            val c = UpdateComponent(system.entities[system.entities.size - 1])
            c.update = update
            system.updateComponents.add(c)
            return this
        }

        fun attachSpriteComponent(material: Material): Builder {
            val entityId = system.entities[system.entities.size - 1]
            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
            val c = SpriteComponent(entityId, material, tr)
            system.spriteComponents.add(c)
            return this
        }

        fun build(scene: Scene, init: (scene: Scene) -> Unit): Long {
            init(scene)
            return system.entities[system.entities.size-1]
        }
    }

    fun findTransformComponent(entityId: Long): TransformComponent? {
        for (e in transformComponents) {
            if (e.entity == entityId) {
                return e
            }
        }

        return null
    }

    fun findUpdateComponent(entityId: Long): UpdateComponent? {
        for (e in updateComponents) {
            if (e.entity == entityId) {
                return e
            }
        }

        return null
    }

    fun findSpriteComponent(entityId: Long): SpriteComponent? {
        for (e in spriteComponents) {
            if (e.entity == entityId) {
                return e
            }
        }

        return null
    }

    fun spriteIterator(): Iterator<SpriteComponent> {
        return spriteComponents.iterator()
    }

    fun updateIterator(): Iterator<UpdateComponent> {
        return updateComponents.iterator()
    }

    private fun uniqueId(): Long {
        if (entityId + 1 >= Long.MAX_VALUE) {
            throw IllegalStateException("There are no more Ids to generate for this entity system!")
        }

        val id = entityId
        entityId += 1

        return id
    }
}
