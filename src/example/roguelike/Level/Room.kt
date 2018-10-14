package example.roguelike.Level

import org.joml.Vector2i

enum class RoomType {
    CAVE,
    PRISON,
}

data class Room(val tiles: List<Vector2i>, val type: RoomType)