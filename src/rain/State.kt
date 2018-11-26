package rain

import rain.api.Input
import rain.api.assertion
import rain.api.gfx.ResourceFactory
import rain.api.gui.Gui
import rain.api.scene.Scene

abstract class State(val stateManager: StateManager) {
    abstract fun init(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input)
    abstract fun update(resourceFactory: ResourceFactory, scene: Scene, gui: Gui, input: Input)
}
