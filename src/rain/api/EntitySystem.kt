package rain.api

import org.joml.Vector2i
import kotlin.IllegalStateException

class EntitySystem<T: Entity> {
    private var entityId: Long = 0
    private var entities = ArrayList<Long>()
    private var transformComponents = ArrayList<TransformComponent>()
    private var updateComponents = ArrayList<UpdateComponent<T>>()
    private var spriteComponents = ArrayList<SpriteComponent>()

    fun newEntity(entity: T): Builder<T> {
        val id = uniqueId()
        entity.setId(id)
        entities.add(id)
        return Builder(id,entity, this)
    }

    class Builder<T: Entity> internal constructor(private var entityId: Long, private val entity: T, private var system: EntitySystem<T>) {
        fun attachTransformComponent(): Builder<T> {
            val c = TransformComponent(system.entities[system.entities.size - 1])
            system.transformComponents.add(c)
            return this
        }

        fun attachUpdateComponent(): Builder<T> {
            val c = UpdateComponent<T>(system.entities[system.entities.size - 1], entity)
            system.updateComponents.add(c)
            return this
        }

        fun attachSpriteComponent(material: Material): Builder<T> {
            val entityId = system.entities[system.entities.size - 1]
            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
            val c = SpriteComponent(entityId, material, tr, Vector2i())
            system.spriteComponents.add(c)
            return this
        }

        fun build(scene: Scene): Long {
            entity.init(scene, system)
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

    fun findUpdateComponent(entityId: Long): UpdateComponent<T>? {
        for (e in updateComponents) {
            if (e.entityHandler == entityId) {
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

    fun updateIterator(): Iterator<UpdateComponent<T>> {
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
