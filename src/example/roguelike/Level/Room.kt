package example.roguelike.Level

import org.joml.Vector2i
import org.joml.Vector4i

enum class RoomType {
    DIRT_CAVE,
    ROCK_CAVE,
    SNOW_CAVE,
}

class Room(val tiles: MutableList<Vector2i>, val area: Vector4i, val type: RoomType) {
    var neighbourRooms = ArrayList<Room>()
}
