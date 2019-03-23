package rain.api.scene.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntermediateEntityInstance constructor(
    @SerialName("pos_x")
    var posX: Float,
    @SerialName("pos_y")
    var posY: Float,
    @SerialName("pos_z")
    var posZ: Float,
    @SerialName("image_x")
    var imageX: Int,
    @SerialName("image_y")
    var imageY: Int,
    var width: Float,
    var height: Float)