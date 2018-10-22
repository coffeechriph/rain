package rain.api

import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import org.joml.Vector2i
import rain.assertion
import kotlin.IllegalStateException
import com.badlogic.gdx.physics.box2d.FixtureDef

class EntitySystem<T: Entity>(val scene: Scene) {
    private var entityId: Long = 0
    private var entities = ArrayList<Long>()
    private var entityWrappers = ArrayList<T?>()
    private var transformComponents = ArrayList<TransformComponent?>()
    private var spriteComponents = ArrayList<SpriteComponent?>()
    private var colliderComponents = ArrayList<Collider?>()

    private var spriteComponentsMap = HashMap<Long, SpriteComponent?>()
    private var transformComponentsMap = HashMap<Long, TransformComponent?>()
    private var colliderComponentsMap = HashMap<Long, Collider?>()
    private var entityWrappersMap = HashMap<Long, T?>()

    fun newEntity(entity: T): Builder<T> {
        val id = uniqueId()
        entity.setId(id)
        entities.add(id)
        entityWrappers.add(entity)
        entityWrappersMap.put(id, entity)
        return Builder(id,entity, this)
    }

    fun clear() {
        entityId = 0
        entities.clear()
        entityWrappers.clear()
        transformComponents.clear()
        spriteComponents.clear()
        spriteComponentsMap.clear()
        transformComponentsMap.clear()
        entityWrappersMap.clear()

        for (collider in colliderComponents) {
            scene.physicWorld.destroyBody(collider!!.getBody())
        }
        colliderComponents.clear()
        colliderComponentsMap.clear()
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

        fun attachBoxColliderComponent(width: Float, height: Float, type: BodyDef.BodyType = BodyDef.BodyType.DynamicBody): Builder<T> {
            system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a collider component is used!")

            if (system.colliderComponentsMap.containsKey(entityId)) {
                assertion("A entity may only have 1 collider component attached at once!")
            }

            val bodyDef = BodyDef()
            bodyDef.type = type
            val body = system.scene.physicWorld.createBody(bodyDef)
            val shape = PolygonShape()
            shape.setAsBox(width / 2.0f, height / 2.0f)

            val fixtureDef = FixtureDef()
            fixtureDef.shape = shape
            fixtureDef.friction = 1.0f
            fixtureDef.density = 1.0f
            fixtureDef.restitution = 0.0f
            body.createFixture(fixtureDef)

            body.userData = entity
            val collider = Collider(body)
            system.colliderComponents.add(collider)
            system.colliderComponentsMap[entityId] = collider
            return this
        }

        fun build(): Long {
            entity.init(system.scene, system)
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

    fun findColliderComponent(entityId: Long): Collider? {
        return colliderComponentsMap[entityId]
    }

    internal fun findEntity(entityId: Long): T? {
        return entityWrappersMap[entityId]
    }

    internal fun getSpriteList(): List<SpriteComponent?> {
        return spriteComponents
    }

    internal fun getEntityList(): List<T?> {
        return entityWrappers
    }

    internal fun getColliderList(): List<Collider?> {
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
