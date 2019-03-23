package rain.api.scene.parse

import kotlinx.serialization.Serializable

@Serializable
data class IntermediateEntity constructor(
    var material: String,
    var metadata: MutableList<IntermediateMetadata>,
    var instances: MutableList<IntermediateEntityInstance>)