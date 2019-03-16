package rain

import rain.api.Input
import rain.api.gfx.ResourceFactory
import rain.api.scene.Scene

abstract class State(val stateManager: StateManager) {
    abstract fun init(resourceFactory: ResourceFactory, scene: Scene)
    abstract fun update(resourceFactory: ResourceFactory, scene: Scene, input: Input, deltaTime: Float)
}
