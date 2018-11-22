package rain.api.scene

import org.joml.Vector2i
import rain.api.assertion

class NavMesh(val width: Int, val height: Int) {
    private class Node(var parent: Node?, val x: Int, val y: Int, var F: Int, var G: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Node

            if (x != other.x) return false
            if (y != other.y) return false

            return true
        }

        override fun hashCode(): Int {
            var result = x
            result = 31 * result + y
            return result
        }
    }

    // Map which indicates wether or not a particular cell is traversable and how slow it is to traverse.
    // 0 = Takes no time to traverse
    // 255 = Impossible to traverse - AI will never attempt to walk on such tiles
    // This allows to have patches of cells where the AI can walk through but will try to avoid
    // Note: The idea here is to have multiple nav meshes where each mesh would allow for a particular
    // unit (for example) to traverse at different places.
    val map = ByteArray(width*height)
    private val nodes = Array(width*height){Node(null,0, 0, 0, 0)}
    private val openList = ArrayList<Node>()
    private val closedList = ArrayList<Node>()

    fun fillMap(data: ByteArray) {
        if (map.size != data.size) {
            assertion("Size of data does not correspond to size of map!")
        }

        for (i in 0 until data.size) {
            map[i] = data[i]
        }
    }

    // Finds a path between origin and destination along the map
    // Returns a list of points that can be interpolated between to traverse the path.
    fun findPath(origin: Vector2i, destination: Vector2i): ArrayList<Vector2i> {
        val originIndex = origin.x + origin.y * width
        val destIndex = destination.x + destination.y * width
        if (originIndex < 0 || originIndex > map.size) {
            assertion("Origin is outside the map! ${origin.x}, ${origin.y}")
        }

        if (destIndex < 0 || destIndex > map.size) {
            assertion("Destination is outside the map! ${destination.x}, ${destination.y}")
        }

        openList.clear()
        closedList.clear()

        var x = 0
        var y = 0
        for (i in 0 until map.size) {
            nodes[i] = Node(null, x, y, 0, 0)
            x += 1
            if (x >= width) {
                x = 0
                y += 1
            }
        }

        openList.add(nodes[origin.x + origin.y * width])

        while (openList.size > 0) {
            openList.sortBy { node -> node.F }
            val current = openList[0]
            openList.removeAt(0)
            closedList.add(current)

            if (current.x == destination.x && current.y == destination.y) {
                break
            }

            val children = getChildren(current)
            for (c in children) {
                if (closedList.contains(c)) {
                    continue
                }

                val nG = current.G + map[c.x + c.y * width]
                val dx = (destination.x - c.x)
                val dy = (destination.y - c.y)
                val nH = Math.abs(dx) + Math.abs(dy)
                val nF = nG + nH

                if (!openList.contains(c)) {
                    c.parent = current
                    c.F = nF
                    openList.add(c)
                }
                else {
                    if (nG > c.G) {
                        continue
                    }
                    else {
                        c.F = nF
                    }
                }
            }
        }

        val finalList = ArrayList<Vector2i>()
        var current: Node? = closedList.last()
        while (current != null) {
            finalList.add(0, Vector2i(current.x, current.y))
            current = current.parent
        }

        return finalList
    }

    private fun getChildren(current: Node): List<Node> {
        val x = current.x
        val y = current.y

        val list = ArrayList<Node>()
        if (x > 0) {
            val index = (x - 1) + y * width
            if (map[index] != 127.toByte()) {
                list.add(nodes[index])
            }

            if (y > 0) {
                val index2 = (x - 1) + (y - 1) * width
                if (map[index2] != 127.toByte()) {
                    list.add(nodes[index2])
                }
            }

            if (y < height - 1) {
                val index2 = (x - 1) + (y + 1) * width
                if (map[index2] != 127.toByte()) {
                    list.add(nodes[index2])
                }
            }
        }

        if (x < width - 1) {
            val index = (x + 1) + y * width
            if (map[index] != 127.toByte()) {
                list.add(nodes[index])
            }

            if (y > 0) {
                val index2 = (x + 1) + (y - 1) * width
                if (map[index2] != 127.toByte()) {
                    list.add(nodes[index2])
                }
            }

            if (y < height - 1) {
                val index2 = (x + 1) + (y + 1) * width
                if (map[index2] != 127.toByte()) {
                    list.add(nodes[index2])
                }
            }
        }

        if (y > 0) {
            val index = x + (y - 1) * width
            if (map[index] != 127.toByte()) {
                list.add(nodes[index])
            }
        }

        if (y < height - 1) {
            val index = x + (y + 1) * width
            if (map[index] != 127.toByte()) {
                list.add(nodes[index])
            }
        }

        return list
    }
}
