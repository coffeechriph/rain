package rain

import rain.api.Input
import rain.api.assertion
import rain.api.gfx.ResourceFactory
import rain.api.gui.Gui
import rain.api.scene.Scene

class StateManager(val resourceFactory: ResourceFactory, val scene: Scene, val gui: Gui, val input: Input) {
    var switchState = false
    val states = HashMap<String, State>()
    private lateinit var currentState: State
    private var nextStateKey: String? = null

    fun startState(key: String) {
        if (states.containsKey(key)) {
            nextStateKey = key
            switchState = true
            return
        }

        assertion("State $key does not exist!")
    }

    fun update() {
        if (::currentState.isInitialized) {
            currentState.update(resourceFactory, scene, gui, input)
        }
    }

    fun initNextState() {
        if (nextStateKey != null) {
            currentState = states[nextStateKey!!]!!
            nextStateKey = null
        }
        currentState.init(resourceFactory, scene, gui, input)
    }
}
