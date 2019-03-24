package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityDefinition constructor(
        var material: String,
        var metadata: MutableList<SceneMetadata>,
        @SerialName("instances")
        var definitionInstances: MutableList<EntityDefinitionInstance>)
