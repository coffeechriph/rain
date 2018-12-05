package rain.api.entity

import com.badlogic.gdx.physics.box2d.Body
import org.joml.Vector2f
import rain.assertion

class Collider internal constructor(private val body: Body, internal val transform: Transform) {
    internal fun getBody(): Body {
        return body
    }

    fun isActive(): Boolean {
        return body.isActive
    }

    fun setActive(c: Boolean) {
        body.isActive = c
    }

    fun applyForceToCenter(x: Float, y: Float) {
        body.applyLinearImpulse(x, y, body.worldCenter.x, body.worldCenter.y, true)
    }

    fun setFriction(friction: Float) {
        if (body.fixtureList.size == 1) {
            body.fixtureList[0].friction = friction
        }
        else {
            assertion("Colliders must have 1 fixture! Zero or multiple are not supported")
        }
    }

    fun setRestitution(restitution: Float) {
        if (body.fixtureList.size == 1) {
            body.fixtureList[0].restitution = restitution
        }
        else {
            assertion("Colliders must have 1 fixture! Zero or multiple are not supported")
        }
    }

    fun setDensity(density: Float) {
        if (body.fixtureList.size == 1) {
            body.fixtureList[0].density = density
        }
        else {
            assertion("Colliders must have 1 fixture! Zero or multiple are not supported")
        }
    }

    fun setDamping(damping: Float) {
       body.linearDamping = damping
    }

    fun setVelocity(x: Float, y: Float) {
        body.setLinearVelocity(x,y)
    }

    fun getVelocity(): Vector2f {
        return Vector2f(body.linearVelocity.x, body.linearVelocity.y)
    }

    fun setPosition(x: Float, y: Float) {
        body.setTransform(x, y, body.angle)
    }

    fun getPosition(): Vector2f {
        return Vector2f(body.position.x, body.position.y)
    }
}
