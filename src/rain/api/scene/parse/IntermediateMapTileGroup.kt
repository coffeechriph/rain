package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntermediateTileGroup constructor(
        @SerialName("image_x")
        val imageX: Int,
        @SerialName("image_y")
        val imageY: Int,
        @SerialName("tile_indices_into_map")
        val tileIndicesIntoMap: MutableSet<Int>)