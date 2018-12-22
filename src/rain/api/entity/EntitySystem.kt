package rain.api.entity

import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import org.joml.Vector2f
import org.joml.Vector2i
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.scene.Scene
import rain.assertion

class EntitySystem<T: Entity>(val scene: Scene) {
    private var entityId: Long = 0
    private var entities = ArrayList<Long>()
    private var entityWrappers = ArrayList<T?>()
    private var transformComponents = ArrayList<Transform?>()
    private var spriteComponents = ArrayList<Sprite?>()
    private var animatorComponents = ArrayList<Animator?>()
    private var colliderComponents = ArrayList<Collider?>()
    private var particleEmitters = ArrayList<ParticleEmitter?>()

    private var spriteComponentsMap = HashMap<Long, Sprite?>()
    private var transformComponentsMap = HashMap<Long, Transform?>()
    private var colliderComponentsMap = HashMap<Long, Collider?>()
    private var animatorComponentsMap = HashMap<Long, Animator?>()
    private var particleEmittersMap = HashMap<Long, ParticleEmitter?>()
    private var entityWrappersMap = HashMap<Long, T?>()

    fun newEntity(entity: T): Builder<T> {
        val id = uniqueId()
        entity.setId(id)
        entities.add(id)
        entityWrappers.add(entity)
        entityWrappersMap.put(id, entity)
        return Builder(id, entity, this)
    }

    fun removeEntity(entity: T) {
        val sprite = spriteComponentsMap[entity.getId()]
        if (sprite != null) {
            spriteComponents.remove(sprite)
            spriteComponentsMap.remove(entity.getId())
        }

        val transform = transformComponentsMap[entity.getId()]
        if (transform != null) {
            transformComponents.remove(transform)
            transformComponentsMap.remove(entity.getId())
        }

        val collider = colliderComponentsMap[entity.getId()]
        if (collider != null) {
            colliderComponents.remove(collider)
            colliderComponentsMap.remove(entity.getId())
        }

        val animator = animatorComponentsMap[entity.getId()]
        if (animator != null) {
            animatorComponents.remove(animator)
            animatorComponentsMap.remove(entity.getId())
        }

        val particleEmitter = particleEmittersMap[entity.getId()]
        if (particleEmitter != null) {
            particleEmitters.remove(particleEmitter)
            particleEmittersMap.remove(entity.getId())
        }

        entityWrappersMap.remove(entity.getId())
        entityWrappers.remove(entity)
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
        animatorComponents.clear()
        animatorComponentsMap.clear()
        particleEmitters.clear()
        particleEmittersMap.clear()

        for (collider in colliderComponents) {
            scene.physicWorld.destroyBody(collider!!.getBody())
        }
        colliderComponents.clear()
        colliderComponentsMap.clear()
    }

    class Builder<T: Entity> internal constructor(private var entityId: Long, private val entity: T, private var system: EntitySystem<T>) {
        // TODO: Take in a parent transform that this transform will follow
        fun attachTransformComponent(): Builder<T> {
            if (system.transformComponentsMap.containsKey(entityId)) {
                assertion("A transform component already exists for entity $entityId!")
            }

            val c = Transform()
            system.transformComponents.add(c)
            system.transformComponentsMap.put(entityId, c)
            return this
        }

        fun attachSpriteComponent(material: Material): Builder<T> {
            if (system.spriteComponentsMap.containsKey(entityId)) {
                assertion("A sprite component already exists for entity $entityId!")
            }

            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
            val c = Sprite(entityId, material, tr, Vector2i())
            system.spriteComponents.add(c)
            system.spriteComponentsMap.put(entityId, c)
            return this
        }

        fun attachAnimatorComponent(): Builder<T> {
            if (system.animatorComponentsMap.containsKey(entityId)) {
                assertion("A animator component already exists for entity $entityId!")
            }

            val spr = system.findSpriteComponent(entityId) ?: throw IllegalStateException("A sprite component must be attached if a animator is used!")
            val animator = Animator(entityId, spr.textureTileOffset)
            system.animatorComponents.add(animator)
            system.animatorComponentsMap.put(entityId, animator)
            return this
        }

        fun attachBoxColliderComponent(width: Float, height: Float, type: BodyDef.BodyType = BodyDef.BodyType.DynamicBody, aliveOnStart: Boolean = true): Builder<T> {
            val transform = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a collider component is used!")

            if (system.colliderComponentsMap.containsKey(entityId)) {
                assertion("A entity may only have 1 collider component attached at once!")
            }

            val bodyDef = BodyDef()
            bodyDef.type = type
            bodyDef.fixedRotation = true
            val body = system.scene.physicWorld.createBody(bodyDef)
            val shape = PolygonShape()
            shape.setAsBox(width / 2.0f, height / 2.0f)

            val fixtureDef = FixtureDef()
            fixtureDef.shape = shape
            fixtureDef.friction = 0.75f
            fixtureDef.density = 1.0f
            fixtureDef.restitution = 0.0f
            body.createFixture(fixtureDef)
            body.isActive = aliveOnStart

            body.userData = entity
            val collider = Collider(body, transform)
            system.colliderComponents.add(collider)
            system.colliderComponentsMap[entityId] = collider
            return this
        }

        fun attachCircleColliderComponent(radius: Float, type: BodyDef.BodyType = BodyDef.BodyType.DynamicBody, aliveOnStart: Boolean = true): Builder<T> {
            val transform = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a collider component is used!")

            if (system.colliderComponentsMap.containsKey(entityId)) {
                assertion("A entity may only have 1 collider component attached at once!")
            }

            val bodyDef = BodyDef()
            bodyDef.type = type
            bodyDef.fixedRotation = true
            val body = system.scene.physicWorld.createBody(bodyDef)
            val shape = CircleShape()
            shape.radius = radius

            val fixtureDef = FixtureDef()
            fixtureDef.shape = shape
            fixtureDef.friction = 0.75f
            fixtureDef.density = 1.0f
            fixtureDef.restitution = 0.0f
            body.createFixture(fixtureDef)
            body.isActive = aliveOnStart

            body.userData = entity
            val collider = Collider(body, transform)
            system.colliderComponents.add(collider)
            system.colliderComponentsMap[entityId] = collider
            return this
        }

        fun attachPolygonColliderComponent(vertices: FloatArray, type: BodyDef.BodyType = BodyDef.BodyType.DynamicBody, aliveOnStart: Boolean = true): Builder<T> {
            val transform = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a collider component is used!")

            if (system.colliderComponentsMap.containsKey(entityId)) {
                assertion("A entity may only have 1 collider component attached at once!")
            }

            val bodyDef = BodyDef()
            bodyDef.type = type
            bodyDef.fixedRotation = true
            val body = system.scene.physicWorld.createBody(bodyDef)
            val shape = PolygonShape()
            shape.set(vertices)

            val fixtureDef = FixtureDef()
            fixtureDef.shape = shape
            fixtureDef.friction = 0.75f
            fixtureDef.density = 1.0f
            fixtureDef.restitution = 0.0f
            body.createFixture(fixtureDef)
            body.isActive = aliveOnStart

            body.userData = entity
            val collider = Collider(body, transform)
            system.colliderComponents.add(collider)
            system.colliderComponentsMap[entityId] = collider
            return this
        }

        fun attachParticleEmitter(resourceFactory: ResourceFactory, numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float): Builder<T> {
            val transform = system.findTransformComponent(entityId) ?: throw IllegalStateException("A transform component must be attached if a particleEmitter component is used!")

            if (system.particleEmittersMap.containsKey(entityId)) {
                assertion("A entity may only have 1 particleEmitter component attached at once!")
            }

            val emitter = ParticleEmitter(resourceFactory, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread)
            system.particleEmitters.add(emitter)
            system.particleEmittersMap[entityId] = emitter
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
            return comp
        }

        return null
    }

    fun findSpriteComponent(entityId: Long): Sprite? {
        return spriteComponentsMap[entityId]
    }

    fun findAnimatorComponent(entityId: Long): Animator? {
        return animatorComponentsMap[entityId]
    }

    fun findColliderComponent(entityId: Long): Collider? {
        return colliderComponentsMap[entityId]
    }

    fun findEmitterComponent(entityId: Long): ParticleEmitter? {
        return particleEmittersMap[entityId]
    }

    internal fun findEntity(entityId: Long): T? {
        return entityWrappersMap[entityId]
    }

    internal fun getSpriteList(): List<Sprite?> {
        return spriteComponents
    }

    internal fun getEntityList(): List<T?> {
        return entityWrappers
    }

    internal fun getColliderList(): List<Collider?> {
        return colliderComponents
    }

    internal fun getAnimatorList(): List<Animator?> {
        return animatorComponents
    }

    internal fun getParticleEmitterList(): List<ParticleEmitter?> {
        return particleEmitters
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
