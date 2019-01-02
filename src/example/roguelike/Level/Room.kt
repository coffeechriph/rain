package example.roguelike.Level

import example.roguelike.Entity.Container
import example.roguelike.Entity.Enemy
import org.joml.Random
import org.joml.Vector2i
import org.joml.Vector4i
import rain.api.entity.Entity

enum class RoomType {
    DIRT_CAVE,
    ROCK_CAVE,
    SNOW_CAVE,
}

class Room(val tiles: MutableList<Vector2i>, val area: Vector4i, val type: RoomType) {
    var neighbourRooms = ArrayList<Room>()

    private val viableTiles = ArrayList<Vector2i>()
    private var allSlotsTaken = false
    private var firstCheck = true

    val enemies = ArrayList<Enemy>()
    val containers = ArrayList<Container>()
    val torches = ArrayList<Entity>()

    fun findNoneEdgeTile(rand: Random): Vector2i? {
        if (viableTiles.size == 0 && !allSlotsTaken) {
            firstCheck = false
            for (tile in tiles) {
                var neighbours = 0
                for (tile2 in tiles) {
                    if (tile == tile2) {
                        continue
                    }

                    if (tile2.x == tile.x && (tile2.y == tile.y - 1 || tile2.y == tile.y + 1)) {
                        neighbours += 1
                    } else if (tile2.y == tile.y && (tile2.x == tile.x - 1 || tile2.x == tile.x + 1)) {
                        neighbours += 1
                    } else if (tile2.x == tile.x - 1 && tile2.y == tile.y - 1) {
                        neighbours += 1
                    } else if (tile2.x == tile.x + 1 && tile2.y == tile.y - 1) {
                        neighbours += 1
                    } else if (tile2.x == tile.x - 1 && tile2.y == tile.y + 1) {
                        neighbours += 1
                    } else if (tile2.x == tile.x + 1 && tile2.y == tile.y + 1) {
                        neighbours += 1
                    }

                    if (neighbours >= 8) {
                        viableTiles.add(tile)
                        break
                    }
                }
            }
        }

        if (viableTiles.isEmpty()) {
            return null
        }

        return viableTiles.removeAt(rand.nextInt(viableTiles.size))
    }
}
