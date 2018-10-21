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
    private var colliderComponents = ArrayList<BoxColliderComponent>()

    private var spriteComponentsMap = HashMap<Long, SpriteComponent>()
    private var transformComponentsMap = HashMap<Long, TransformComponent>()
    private var colliderComponentsMap = HashMap<Long, BoxColliderComponent>()
    private var entityWrappersMap = HashMap<Long, T>()

    fun newEntity(entity: T): Builder<T> {
        val id = uniqueId()
        entity.setId(id)
        entities.add(id)
        entityWrappers.add(entity)
        entityWrappersMap.put(id, entity)
        return Builder(id,entity, this)
    }

    class Builder<T: Entity> internal constructor(private var entityId: Long, private val entity: T, private var system: EntitySystem<T>) {
        // TODO: Take in a parent transform that this transform will follow
        fun attachTransformComponent(): Builder<T> {
            val c = TransformComponent(entityId)
            system.transformComponents.add(c)
            system.transformComponentsMap.put(entityId, c)
            return this
        }

        fun attachSpriteComponent(material: Material): Builder<T> {
            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
            val c = SpriteComponent(entityId, material, tr, Vector2i())
            system.spriteComponents.add(c)
            system.spriteComponentsMap.put(entityId, c)
            return this
        }

        fun attachBoxColliderComponent(width: Float, height: Float, tag: String = "none"): Builder<T> {
            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a collider component is used!")
            val c = BoxColliderComponent(entityId, tag, tr.x, tr.y, width, height)
            system.colliderComponents.add(c)
            system.colliderComponentsMap.put(entityId, c)
            return this
        }

        fun build(scene: Scene): Long {
            entity.init(scene, system)
            return system.entities[system.entities.size-1]
        }
    }

    fun findTransformComponent(entityId: Long): Transform? {
        val comp = transformComponentsMap[entityId]
        if (comp != null) {
            return comp.transform
        }

        return null
    }

    fun findSpriteComponent(entityId: Long): SpriteComponent? {
        return spriteComponentsMap[entityId]
    }

    fun findBoxColliderComponent(entityId: Long): BoxColliderComponent? {
        return colliderComponentsMap[entityId]
    }

    internal fun findEntity(entityId: Long): T? {
        return entityWrappersMap[entityId]
    }

    internal fun getSpriteList(): List<SpriteComponent> {
        return spriteComponents
    }

    internal fun getEntityList(): List<T> {
        return entityWrappers;
    }

    internal fun getColliderList(): List<BoxColliderComponent> {
        return colliderComponents
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
