package rain.api.entity

import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import org.joml.Vector2f
import org.joml.Vector2i
import rain.api.gfx.*
import rain.api.scene.Scene
import rain.assertion

const val pixelsToMeters = 1.0f / 64.0f
const val metersToPixels = 64.0f / 1.0f

class EntitySystem<T: Entity>(val scene: Scene, val material: Material?) {
    private var entities = ArrayList<Long>()
    private var entityWrappers = ArrayList<T?>()
    private var transformComponents = ArrayList<Transform?>()
    private var spriteComponents = ArrayList<Sprite?>()
    private var animatorComponents = ArrayList<Animator?>()
    private var colliderComponents = ArrayList<Collider?>()

    private var spriteComponentsMap = HashMap<Long, Sprite?>()
    private var transformComponentsMap = HashMap<Long, Transform?>()
    private var colliderComponentsMap = HashMap<Long, Collider?>()
    private var animatorComponentsMap = HashMap<Long, Animator?>()
    private var entityWrappersMap = HashMap<Long, T?>()

    fun newEntity(entity: T): Builder<T> {
        entities.add(entity.getId())
        entityWrappers.add(entity)
        entityWrappersMap[entity.getId()] = entity
        return Builder(entity.getId(), entity, this)
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

        emitterManagerRemoveEmitterByEntity(entity.getId())
        emitterManagerRemoveBurstEmitterByEntity(entity.getId())
        renderManagerRemoveRenderComponentByEntity(entity.getId())

        entityWrappersMap.remove(entity.getId())
        entityWrappers.remove(entity)
    }

    fun clear() {
        for (entity in entities) {
            emitterManagerRemoveEmitterByEntity(entity)
            emitterManagerRemoveBurstEmitterByEntity(entity)
            renderManagerRemoveRenderComponentByEntity(entity)
        }

        entities.clear()
        entityWrappers.clear()
        transformComponents.clear()
        spriteComponents.clear()
        spriteComponentsMap.clear()
        transformComponentsMap.clear()
        entityWrappersMap.clear()
        animatorComponents.clear()
        animatorComponentsMap.clear()

        for (collider in colliderComponents) {
            scene.physicWorld.destroyBody(collider!!.getBody())
        }
        colliderComponents.clear()
        colliderComponentsMap.clear()
    }

    // TODO: All these attaches shouldn't care about order
    // ánd should be "self-contained" meaning that if transform is missing then the
    // renderer would just use a default transform.
    class Builder<T: Entity> internal constructor(private var entityId: Long, private val entity: T, private var system: EntitySystem<T>) {
        // TODO: Take in a parent transform that this transform will follow
        fun attachTransformComponent(): Builder<T> {
            if (system.transformComponentsMap.containsKey(entityId)) {
                assertion("A transform component already exists for entity $entityId!")
            }

            val c = Transform()
            system.transformComponents.add(c)
            system.transformComponentsMap[entityId] = c
            return this
        }

        fun attachSpriteComponent(): Builder<T> {
            if (system.spriteComponentsMap.containsKey(entityId)) {
                assertion("A sprite component already exists for entity $entityId!")
            }
            if (system.material == null) {
                assertion("Sprite components may not be attached to a system without a material!")
            }

            val tr = system.findTransformComponent(entityId)
                    ?: throw IllegalStateException("A transform component must be attached if a sprite component is used!")
            val c = Sprite(entityId, tr, Vector2i())
            system.spriteComponents.add(c)
            system.spriteComponentsMap[entityId] = c
            return this
        }

        fun attachAnimatorComponent(): Builder<T> {
            if (system.animatorComponentsMap.containsKey(entityId)) {
                assertion("A animator component already exists for entity $entityId!")
            }

            var textureTileOffset: Vector2i? = null
            val spr = system.findSpriteComponent(entityId)
            if (spr != null) {
                textureTileOffset = spr.textureTileOffset
            }
            else {
                val rc = renderManagerGetRenderComponentByEntity(entityId)
                if (rc != null) {
                    // TODO: If multiple renderComponents are added then there's not way to
                    // specify which one is in charge of the textureTileOffsets...
                    textureTileOffset = rc[0].textureTileOffset
                }
                else {
                    throw IllegalStateException("Must have either Sprite or RenderComponent attached in order to use a Animator!")
                }
            }

            val animator = Animator(entityId, textureTileOffset)
            system.animatorComponents.add(animator)
            system.animatorComponentsMap[entityId] = animator
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
            shape.setAsBox(width / 2.0f * pixelsToMeters, height / 2.0f * pixelsToMeters)

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
            shape.radius = radius * pixelsToMeters

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
            // TODO: Adjust verticies to meters
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

        fun attachParticleEmitter(numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float, tickRate: Float = 1.0f): Builder<T> {
            val transform = system.findTransformComponent(entityId) ?: throw IllegalStateException("A transform component must be attached if a particleEmitter component is used!")
            emitterManagerCreateEmitter(entityId, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread, tickRate)
            return this
        }

        fun attachBurstParticleEmitter(numParticles: Int, particleSize: Float, particleLifetime: Float, velocity: Vector2f, directionType: DirectionType, spread: Float, tickRate: Float = 1.0f): Builder<T> {
            val transform = system.findTransformComponent(entityId) ?: throw IllegalStateException("A transform component must be attached if a particleEmitter component is used!")
            emitterManagerCreateBurstEmitter(entityId, transform, numParticles, particleSize, particleLifetime, velocity, directionType, spread, tickRate)
            return this
        }

        fun attachRenderComponent(material: Material, mesh: Mesh): Builder<T> {
            val transform = system.findTransformComponent(entityId) ?: throw IllegalStateException("A transform component must be attached if a particleEmitter component is used!")
            val component = RenderComponent(transform, mesh, material)
            renderManagerAddRenderComponent(entityId, component)
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

    internal fun findEntity(entityId: Long): T? {
        return entityWrappersMap[entityId]
    }

    /*
        TODO: EntityWrappers should not be super necessary!
        What we should do is supply the users with tools to build logic for entities
        through controlled efficient components.
            1. MoveComponent (Access to a entities Transform & Input)
            2. PathfindComponent (Access to a entities target, transform and path)
            3. ReactComponent<List<T: Entity>> (Access to a entities Transform, Input and a list of other entities)
     */
    internal fun getSpriteList(): List<Sprite?> {
        return spriteComponents
    }

    fun getEntityList(): List<T?> {
        return entityWrappers
    }

    internal fun getColliderList(): List<Collider?> {
        return colliderComponents
    }

    internal fun getAnimatorList(): List<Animator?> {
        return animatorComponents
    }
}
