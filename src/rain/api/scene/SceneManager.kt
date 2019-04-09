package rain.api.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.World
import org.joml.Random
import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.WindowContext
import rain.api.entity.Entity
import rain.api.entity.ParticleEmitterEntity
import rain.api.gfx.*
import rain.api.gui.v2.guiManagerClear
import rain.api.manager.*
import rain.vulkan.VertexAttribute

// TODO: The window should not be accessible from the scene ...
class SceneManager(val resourceFactory: ResourceFactory) {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val spriteBatchers = ArrayList<SpriteBatcher>()
    lateinit var physicWorld: World
        private set
    private var physicsContactListener = PhysicsContactListener()

    private lateinit var activeScene: Scene
    private var switchScene = false
    private var nextScene: Scene? = null

    fun setScene(scene: Scene) {
        switchScene = true
        nextScene = scene
    }

    internal fun init() {
        val vertices = floatArrayOf(
                -0.5f, -0.5f, 0.0f, 0.0f,
                -0.5f, 0.5f, 0.0f, 1.0f,
                0.5f, 0.5f, 1.0f, 1.0f,

                0.5f, 0.5f, 1.0f, 1.0f,
                0.5f, -0.5f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.0f, 0.0f
        )
        // TODO: Optimize this
        val byteBuffer = memAlloc(24*4)
        byteBuffer.asFloatBuffer().put(vertices).flip()
        this.quadVertexBuffer = resourceFactory.buildVertexBuffer()
                .withVertices(byteBuffer)
                .withState(VertexBufferState.STATIC)
                .withAttribute(VertexAttribute(0, 2))
                .withAttribute(VertexAttribute(1, 2))
                .build()

        Box2D.init()
        physicWorld = World(Vector2(0.0f, 0.0f), true)
        physicWorld.setContactListener(physicsContactListener)
    }

    internal fun update(input: Input, renderer: Renderer) {
        physicWorld.step(1.0f / 60.0f, 6, 2)
        physicWorld.clearForces()

        if (::activeScene.isInitialized) {
            activeScene.doUpdate(input, renderer)
        }
    }

    internal fun checkSceneSwitch() {
        if (switchScene) {
            if (::activeScene.isInitialized) {
                activeScene.clear()

                // TODO: This should be clearable through whatever we put inside the scene
                // TODO: Make the UI part of the scene
                guiManagerClear()
                emitterManagerClear()
            }

            activeScene = nextScene!!
            activeScene.init()

            switchScene = false
            nextScene = null
        }
    }

    // TODO: Make sure we destroy the vertex buffers as well
    internal fun destroy() {
        if (::activeScene.isInitialized) {
            activeScene.clear()
        }
    }
}
