package example.roguelike.Level

import org.joml.Vector2i

enum class RoomType {
    CAVE,
    PRISON,
}

class Room(val tiles: MutableList<Vector2i>, val type: RoomType) {
    var connectedRooms = ArrayList<Int>()
}
