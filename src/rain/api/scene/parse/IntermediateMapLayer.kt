package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TilemapLayer constructor(
        @SerialName("tile_group")
        var intermediateTileGroup: MutableList<IntermediateTileGroup>,
        @SerialName("metadata")
        var metadata: MutableList<IntermediateMetadata>)