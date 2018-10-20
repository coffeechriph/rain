package rain.api

import org.joml.Vector2i
import java.util.function.Consumer
import kotlin.IllegalStateException

class EntitySystem<T: Entity> {
    private var entityId: Long = 0
    private var entities = ArrayList<Long>()
    private var entityWrappers = ArrayList<T>()
    private var transformComponents = ArrayList<TransformComponent>()
    private var spriteComponents = ArrayList<SpriteComponent>()

    private var spriteComponentsMap = HashMap<Long, SpriteComponent>()
    private var transformComponentsMap = HashMap<Long, TransformComponent>()

    fun newEntity(entity: T): Builder<T> {
        val id = uniqueId()
        entity.setId(id)
        entities.add(id)
        entityWrappers.add(entity)
        return Builder(id,entity, this)
    }

    class Builder<T: Entity> internal constructor(private var entityId: Long, private val entity: T, private var system: EntitySystem<T>) {
        fun attachTransformComponent(): Builder<T> {
            val c = TransformComponent(entityId)
            system.transformComponents.add(c)
            system.transformComponentsMap.put(entityId, c)
            return this
        }

        fun attachSpriteComponent(material: Material): Builder<T> {
            val entityId = system.entities[system.entities.size - 1]
            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
            val c = SpriteComponent(entityId, material, tr, Vector2i())
            system.spriteComponents.add(c)
            system.spriteComponentsMap.put(entityId, c)
            return this
        }

        fun build(scene: Scene): Long {
            entity.init(scene, system)
            return system.entities[system.entities.size-1]
        }
    }

    fun findTransformComponent(entityId: Long): Transform? {
        val comp = transformComponentsMap.get(entityId)
        if (comp != null) {
            return comp.transform
        }

        return null
    }

    fun findSpriteComponent(entityId: Long): SpriteComponent? {
        return spriteComponentsMap.get(entityId)
    }

    fun getSpriteList(): List<SpriteComponent> {
        return spriteComponents
    }

    fun getEntityList(): List<T> {
        return entityWrappers;
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
