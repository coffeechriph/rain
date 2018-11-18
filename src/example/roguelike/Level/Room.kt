package example.roguelike.Level

import org.joml.Vector2i
import org.joml.Vector4i
import java.util.*

enum class RoomType {
    DIRT_CAVE,
    ROCK_CAVE,
    SNOW_CAVE,
}

class Room(val tiles: MutableList<Vector2i>, val area: Vector4i, val type: RoomType) {
    var neighbourRooms = ArrayList<Room>()

    fun findNoneEdgeTile(rand: Random): Vector2i {
        val viableTiles = ArrayList<Vector2i>()
        for (tile in tiles) {
            var neighbours = 0
            for (tile2 in tiles) {
                if (tile == tile2) {
                    continue
                }

                if (tile2.x == tile.x && (tile2.y == tile.y - 1 || tile2.y == tile.y + 1)) {
                    neighbours += 1
                }

                if (tile2.y == tile.y && (tile2.x == tile.x - 1 || tile2.x == tile.x + 1)) {
                    neighbours += 1
                }

                if (tile2.x == tile.x - 1 && tile2.y == tile.y - 1) {
                    neighbours += 1
                }
                else if (tile2.x == tile.x + 1 && tile2.y == tile.y - 1) {
                    neighbours += 1
                }
                else if (tile2.x == tile.x - 1 && tile2.y == tile.y + 1) {
                    neighbours += 1
                }
                else if (tile2.x == tile.x + 1 && tile2.y == tile.y + 1)  {
                    neighbours += 1
                }
            }

            if (neighbours >= 8) {
                viableTiles.add(tile)
            }
        }

        return viableTiles[rand.nextInt(viableTiles.size)]
    }
}
