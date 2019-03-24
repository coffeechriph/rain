package rain.api.scene.parse

interface SceneLoader {
    fun load(file: String): SceneDefinition
    fun save(file: String, sceneDefinition: SceneDefinition)
}
