package rain.api.components

import com.badlogic.gdx.physics.box2d.Body
import org.joml.Vector2f
import rain.api.entity.metersToPixels
import rain.api.entity.pixelsToMeters
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

    fun applyLinearImpulseToCenter(x: Float, y: Float) {
        body.applyLinearImpulse(x * pixelsToMeters, y * pixelsToMeters, body.worldCenter.x, body.worldCenter.y, true)
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
        body.setLinearVelocity(x * pixelsToMeters, y * pixelsToMeters)
    }

    fun getVelocity(): Vector2f {
        return Vector2f(body.linearVelocity.x * metersToPixels, body.linearVelocity.y * metersToPixels)
    }

    fun setPosition(x: Float, y: Float) {
        body.setTransform(x * pixelsToMeters, y * pixelsToMeters, body.angle)
    }

    fun getPosition(): Vector2f {
        return Vector2f(body.position.x * metersToPixels, body.position.y * metersToPixels)
    }
}
