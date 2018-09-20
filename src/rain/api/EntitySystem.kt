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
class EntitySystem<T: Entity> {
    private var entityId: Long = 0
    private lateinit var entities: MutableList<T>
    private lateinit var transformComponents: MutableList<TransformComponent>
    private lateinit var updateComponents: MutableList<UpdateComponent>
    private lateinit var spriteComponents: MutableList<SpriteComponent>

    fun newInstance(factory: (id: Long) -> T): EntitySystem<T> {
        val e: T = factory(uniqueId())
        entities.add(e)
        return this
    }

    fun attachTransformComponent(): EntitySystem<T> {
        val c = TransformComponent(entities[entities.size-1].entityId)
        transformComponents.add(c)
        return this
    }

    fun attachUpdateComponent(): EntitySystem<T> {
        val c = UpdateComponent(entities[entities.size-1].entityId)
        updateComponents.add(c)
        return this
    }

    fun attachSpriteComponent(material: Material): EntitySystem<T> {
        val entityId = entities[entities.size - 1].entityId
        val tr = findTransformComponent(entityId) ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
        val c = SpriteComponent(entityId, material, tr)
        spriteComponents.add(c)
        return this
    }

    fun findTransformComponent(entityId: Long): TransformComponent? {
        for (e in transformComponents) {
            if (e.entity == entityId) {
                return e
            }
        }

        return null
    }

    fun findEntity(entityId: Long): Entity? {
        for (e in entities) {
            if (e.entityId == entityId) {
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
