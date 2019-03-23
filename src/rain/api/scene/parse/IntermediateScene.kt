package rain.api.scene.parse

interface IntermediateScene {
    fun load(file: String)
    fun getEntities(): List<IntermediateEntity>
    fun getMap(): IntermediateMap
}