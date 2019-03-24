package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SceneDefinition constructor(
        @SerialName("map")
        val map: MutableList<MapDefinition>,
        @SerialName("entities")
        val entities: MutableMap<String, EntityDefinition>)
