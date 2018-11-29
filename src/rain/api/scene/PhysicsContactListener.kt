package rain.api.scene

import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import rain.api.entity.Entity
import rain.assertion

class PhysicsContactListener : ContactListener {
    override fun endContact(contact: Contact?) {
    }

    override fun beginContact(contact: Contact?) {
        val con = contact!!
        if(con.fixtureA.body.userData !is Entity && con.fixtureB.body.userData !is Entity) {
            assertion("Colliders must have a Entity attached as UserData!")
        }

        val entity1 = con.fixtureA.body.userData as Entity
        val entity2 = con.fixtureB.body.userData as Entity

        entity1.onCollision(entity2)
        entity2.onCollision(entity1)
    }

    override fun preSolve(contact: Contact?, oldManifold: Manifold?) {
    }

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {
    }

}
