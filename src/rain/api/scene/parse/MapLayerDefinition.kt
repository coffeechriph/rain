package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapLayerDefinition constructor(
        @SerialName("tile_group")
        var mapLayerTileGroup: MutableList<MapLayerTileGroup>,
        @SerialName("metadata")
        var metadata: MutableList<SceneMetadata>)
