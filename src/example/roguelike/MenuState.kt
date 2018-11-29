package example.roguelike

import org.joml.Vector2f
import rain.State
import rain.StateManager
import rain.api.Input
import rain.api.gfx.ResourceFactory
import rain.api.gui.Button
import rain.api.gui.Container
import rain.api.gui.Gui
import rain.api.scene.Camera
import rain.api.scene.Scene

class MenuState(stateManager: StateManager): State(stateManager) {
    private var camera = Camera(Vector2f(0.0f, 20.0f))
    private lateinit var menuContainer: Container
    private lateinit var startGameButton: Button
    private lateinit var settingsButton: Button
    private lateinit var exitButton: Button

    private var buttonsAnimation = 0.0f

    override fun init(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input) {
        scene.setActiveCamera(camera)

        menuContainer = gui.newContainer(0.0f, 0.0f, 1280.0f, 720.0f)
        startGameButton = Button()
        startGameButton.x = 1280.0f / 2.0f - 100.0f
        startGameButton.y = 240.0f
        startGameButton.w = 200.0f
        startGameButton.h = 60.0f
        startGameButton.text = "New Game"
        menuContainer.addComponent(startGameButton)

        settingsButton = Button()
        settingsButton.x = 1280.0f / 2.0f - 100.0f
        settingsButton.y = 330.0f
        settingsButton.w = 200.0f
        settingsButton.h = 60.0f
        settingsButton.text = "Settings"
        menuContainer.addComponent(settingsButton)

        exitButton= Button()
        exitButton.x = 1280.0f / 2.0f - 100.0f
        exitButton.y = 420.0f
        exitButton.w = 200.0f
        exitButton.h = 60.0f
        exitButton.text = "Exit"
        menuContainer.addComponent(exitButton)
    }

    override fun update(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input) {
        val scale = Math.sin(buttonsAnimation.toDouble()).toFloat() * 7.0f
        startGameButton.w = 200.0f + scale
        startGameButton.h = 60.0f + scale
        startGameButton.x = 1280.0f / 2.0f - 100.0f - scale / 2.0f
        startGameButton.y = 240.0f - scale / 2.0f

        settingsButton.w = 200.0f + scale
        settingsButton.h = 60.0f + scale
        settingsButton.x = 1280.0f / 2.0f - 100.0f - scale / 2.0f
        settingsButton.y = 330.0f - scale / 2.0f

        exitButton.w = 200.0f + scale
        exitButton.h = 60.0f + scale
        exitButton.x = 1280.0f / 2.0f - 100.0f - scale / 2.0f
        exitButton.y = 420.0f - scale / 2.0f

        buttonsAnimation += 0.02f
        if (buttonsAnimation >= Math.PI*2) {
            buttonsAnimation = 0.0f
        }
        menuContainer.isDirty = true

        if (startGameButton.active) {
            stateManager.startState("game")
        }
        else if (settingsButton.active) {

        }
        else if (exitButton.active) {

        }
    }
}
