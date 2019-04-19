package rain.api.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.World
import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.Input
import rain.api.WindowContext
import rain.api.gfx.*
import rain.api.gui.v2.guiManagerClear
import rain.api.manager.emitterManagerClear
import rain.vulkan.VertexAttribute

// TODO: The window should not be accessible from the scene ...
class SceneManager(val resourceFactory: ResourceFactory) {
    private lateinit var quadVertexBuffer: VertexBuffer
    private val spriteBatchers = ArrayList<SpriteBatcher>()
    lateinit var physicWorld: World
        private set
    private var physicsContactListener = PhysicsContactListener()
    var activeCamera = Camera(1000.0f, Vector2i(1280, 720))

    private val loadScenes = ArrayList<Scene>()
    private val activeScenes = ArrayList<Scene>()
    private val unloadScenes = ArrayList<Scene>()

    fun loadScene(scene: Scene) {
        loadScenes.add(scene)
    }

    fun unloadScene(scene: Scene) {
        unloadScenes.add(scene)
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

    internal fun update(window: WindowContext, input: Input, renderer: Renderer) {
        window.cameraProjectionSize = activeCamera.resolution
        renderer.setActiveCamera(activeCamera)

        physicWorld.step(1.0f / 60.0f, 6, 2)
        physicWorld.clearForces()

        for (scene in activeScenes) {
            scene.doUpdate(input)
        }
    }

    internal fun manageScenes() {
        for (scene in unloadScenes) {
            scene.clear()
            guiManagerClear()
            emitterManagerClear()
            activeScenes.remove(scene)
        }
        unloadScenes.clear()

        for (scene in loadScenes) {
            scene.init()
            activeScenes.add(scene)
        }
        loadScenes.clear()
    }

    // TODO: Make sure we destroy the vertex buffers as well
    internal fun destroy() {
        for (scene in activeScenes) {
            scene.clear()
        }
        activeScenes.clear()
    }
}
