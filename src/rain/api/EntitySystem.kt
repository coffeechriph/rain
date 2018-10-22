package rain.api

import com.badlogic.gdx.physics.box2d.Body
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
    private var bodyComponents = ArrayList<Body?>()

    private var spriteComponentsMap = HashMap<Long, SpriteComponent?>()
    private var transformComponentsMap = HashMap<Long, TransformComponent?>()
    private var bodyComponentsMap = HashMap<Long, Body?>()
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

        for (body in bodyComponents) {
            scene.physicWorld.destroyBody(body!!)
        }
        bodyComponents.clear()
        bodyComponentsMap.clear()
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

        fun attachBoxColliderComponent(x: Float = 0.0f, y: Float = 0.0f, width: Float, height: Float, density: Float = 1.0f, friction: Float = 1.0f,
                                       linearDamping: Float = 1.0f, type: BodyDef.BodyType = BodyDef.BodyType.DynamicBody): Builder<T> {
            system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a collider component is used!")

            if (system.bodyComponentsMap.containsKey(entityId)) {
                assertion("A entity may only have 1 collider component attached at once!")
            }

            val bodyDef = BodyDef()
            bodyDef.position.set(x, y)
            bodyDef.type = type
            val body = system.scene.physicWorld.createBody(bodyDef)
            val shape = PolygonShape()
            shape.setAsBox(width / 2.0f, height / 2.0f)

            val fixtureDef = FixtureDef()
            fixtureDef.shape = shape
            fixtureDef.density = density
            fixtureDef.friction = friction
            fixtureDef.restitution = 0.0f
            body.linearDamping = linearDamping
            body.createFixture(fixtureDef)

            body.userData = entity
            system.bodyComponents.add(body)
            system.bodyComponentsMap[entityId] = body
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

    fun findBodyComponent(entityId: Long): Body? {
        return bodyComponentsMap.get(entityId)
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

    internal fun getBodyList(): List<Body?> {
        return bodyComponents
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
