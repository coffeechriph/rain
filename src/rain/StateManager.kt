package rain

import rain.api.Input
import rain.api.gfx.ResourceFactory
import rain.api.scene.Scene
class StateManager(val resourceFactory: ResourceFactory, val scene: Scene) {
    var switchState = false
    val states = HashMap<String, State>()
    private lateinit var currentState: State
    private var nextStateKey: String? = null
    private var exit = false

    fun exitState() {
        log("Call to exit!")
        exit = true
        switchState = true
    }

    fun startState(key: String) {
        if (states.containsKey(key)) {
            log("Prepare to switch to state $key.")
            nextStateKey = key
            switchState = true
            return
        }

        assertion("State $key does not exist!")
    }

    fun update(input: Input) {
        if (::currentState.isInitialized) {
            currentState.update(resourceFactory, scene, input)
        }
    }

    fun initNextState(): Boolean {
        if (exit) {
            return true
        }

        if (nextStateKey != null) {
            currentState = states[nextStateKey!!]!!
            log("Next state set to $nextStateKey")
        }
        else {
            assertion("NextStateKey is null!")
        }

        currentState.init(resourceFactory, scene)
        log("State $nextStateKey initialized!")
        nextStateKey = null
        return false
    }
}

