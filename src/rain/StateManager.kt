package rain

import rain.api.Input
import rain.api.assertion
import rain.api.gfx.ResourceFactory
import rain.api.gui.Gui
import rain.api.scene.Scene

class StateManager(val resourceFactory: ResourceFactory, val scene: Scene, val gui: Gui, val input: Input) {
    var switchState = false
        get() {
            switchState = !field
            return !field
        }
        private set

    val states = HashMap<String, State>()
    private var currentState: State? = null

    fun startState(key: String) {
        switchState = true
        if (states.containsKey(key)) {
            currentState = states[key]!!
            currentState!!.init(resourceFactory, scene, gui, input)
            return
        }

        assertion("State $key does not exist!")
    }

    fun update() {
        currentState!!.update(resourceFactory, scene, gui, input)
    }
}
