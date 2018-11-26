package example.roguelike

import rain.State
import rain.StateManager
import rain.api.Input
import rain.api.gfx.ResourceFactory
import rain.api.gui.Gui
import rain.api.scene.Scene

class MenuState(stateManager: StateManager): State(stateManager) {
    override fun init(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input) {
    }

    override fun update(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input) {
        if (input.keyState(Input.Key.KEY_SPACE) == Input.InputState.PRESSED) {
            stateManager.startState("game")
        }
    }
}
