package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntermediateMap constructor(
        @SerialName("tile_num_x")
        var tileNumX: Int,
        @SerialName("tile_num_y")
        var tileNumY: Int,
        @SerialName("tile_width")
        var tileWidth: Float,
        @SerialName("tile_height")
        var tileHeight: Float,
        @SerialName("layers")
        var layers: MutableList<TilemapLayer>)
