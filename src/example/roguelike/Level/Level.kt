package example.roguelike.Level

import example.roguelike.Entity.Enemy
import example.roguelike.Entity.Krac
import example.roguelike.Entity.Player
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4i
import rain.api.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.IllegalStateException
import kotlin.collections.ArrayList
import kotlin.math.sign

class Level {
    lateinit var map: IntArray
        private set
    var mapWidth = 0
        private set
    var mapHeight = 0
        private set
    var width = 0
        private set
    var height = 0
        private set
    var maxCellX = 0
        private set
    var maxCellY = 0
        private set
    var backTilemap = Tilemap()
        private set
    var frontTilemap = Tilemap()
        private set

    private lateinit var material: Material
    private lateinit var texture: Texture2d
    private var firstBuild = true

    private var mapBackIndices = Array(0){ TileIndex(0,0)}
    private var mapFrontIndices = Array(0){TileIndex(0,0)}
    private var rooms = ArrayList<Room>()
    private var enemies = ArrayList<Enemy>()
    private var enemySystem = EntitySystem<Enemy>()
    private lateinit var enemyMaterial: Material

    fun update(player: Player) {
        for (enemy in enemies) {
            enemySystem.findSpriteComponent(enemy.getId())!!.visible = enemy.cellX == player.cellX && enemy.cellY == player.cellY
        }
    }

    fun getFirstTilePos(): Vector2i {
        return Vector2i(rooms[0].tiles[0].x * 64, rooms[0].tiles[0].y * 64)
    }

    fun create(resourceFactory: ResourceFactory, scene: Scene, mapWidth: Int, mapHeight: Int, width: Int, height: Int) {
        maxCellX = mapWidth / width
        maxCellY = mapHeight / height
        texture = resourceFactory.createTexture2d("./data/textures/tiles.png", TextureFilter.NEAREST)
        texture.setTiledTexture(16,16)
        material = resourceFactory.createMaterial("./data/shaders/tilemap.vert.spv", "./data/shaders/basic.frag.spv", texture, Vector3f(1.0f,1.0f, 1.0f))

        this.mapWidth = mapWidth
        this.mapHeight = mapHeight
        this.width = width
        this.height = height
        map = IntArray(mapWidth*mapHeight)

        val enemyTexture = resourceFactory.createTexture2d("./data/textures/krac.png", TextureFilter.NEAREST)
        enemyTexture.setTiledTexture(32,32)
        enemyMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", enemyTexture, Vector3f(1.0f, 1.0f, 1.0f))
        scene.addSystem(enemySystem)
    }

    fun switchCell(resourceFactory: ResourceFactory, cellX: Int, cellY: Int) {
        val backIndices = Array(width*height){ TileIndexNone }
        val frontIndices = Array(width*height){ TileIndexNone }
        var sx = cellX * width
        var sy = cellY * height
        for (i in 0 until width*height) {
            backIndices[i] = mapBackIndices[sx + sy*mapWidth]
            frontIndices[i] = mapFrontIndices[sx + sy*mapWidth]

            sx += 1
            if (sx >= cellX * width + width) {
                sx = cellX * width
                sy += 1
            }
        }

        if (firstBuild) {
            backTilemap.create(resourceFactory, material, width, height, 64.0f, 64.0f, backIndices)
            frontTilemap.create(resourceFactory, material, width, height, 64.0f, 64.0f, frontIndices)
            backTilemap.update(resourceFactory, backIndices)
            frontTilemap.update(resourceFactory, frontIndices)

            backTilemap.getTransform().setPosition(0.0f, 0.0f, 1.0f)
            frontTilemap.getTransform().setPosition(0.0f, 0.0f, 10.0f)
            firstBuild = false
        }
        else {
            backTilemap.update(resourceFactory, backIndices)
            frontTilemap.update(resourceFactory, frontIndices)
        }
    }

    fun build(resourceFactory: ResourceFactory, scene: Scene, seed: Long) {
        generate(seed, 7)
        buildRooms()

        mapBackIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapFrontIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        populateTilemap()

        saveMapAsImage("map.png")
        switchCell(resourceFactory, 0, 0)
        generateEnemies(scene)
    }

    private fun populateTilemap() {
        for (room in rooms) {
            val tileY = room.type.ordinal

            for (tile in room.tiles) {
                val index = tile.x + tile.y * mapWidth
                mapBackIndices[index] = TileIndex(0, tileY)

                if (tile.y > 0) {
                    if (map[tile.x + (tile.y-1)*mapWidth] == 1) {
                        mapBackIndices[tile.x + (tile.y - 1) * mapWidth] = TileIndex(1, tileY)

                        if (tile.y > 1) {
                            if (map[tile.x + (tile.y-2)*mapWidth] == 1) {
                                mapFrontIndices[tile.x + (tile.y - 2) * mapWidth] = TileIndex(3,tileY)
                            }
                        }
                    }
                }

                if (tile.y < mapHeight - 1) {
                    if (map[tile.x + (tile.y+1)*mapWidth] == 1) {
                        if(tile.y < mapHeight - 2 && map[tile.x + (tile.y+2)*mapWidth] == 1) {
                            mapFrontIndices[tile.x + (tile.y + 1) * mapWidth] = TileIndex(2, 1)
                        }
                        else {
                            map[tile.x + tile.y * mapWidth] = 1
                            mapFrontIndices[tile.x + tile.y * mapWidth] = TileIndex(2, 1)
                            mapBackIndices[tile.x + (tile.y + 1) * mapWidth] = TileIndex(3, tileY)
                        }
                    }

                    if (tile.x > 0) {
                        if (map[(tile.x - 1) + tile.y*mapWidth] == 1 &&
                            map[(tile.x - 1) + (tile.y+1)*mapWidth] == 1) {
                            mapFrontIndices[(tile.x-1) + tile.y*mapWidth] = TileIndex(2,1)
                        }
                    }

                    if (tile.x < mapWidth - 1) {
                        if (map[(tile.x + 1) + tile.y*mapWidth] == 1 &&
                                map[(tile.x + 1) + (tile.y+1)*mapWidth] == 1) {
                            mapFrontIndices[(tile.x+1) + tile.y*mapWidth] = TileIndex(2,1)
                        }
                    }
                }
            }
        }
    }

    private fun generate(seed: Long, repeats: Int) {
        var x = 0
        var y = 0
        val random = Random(seed)
        for (i in 0 until map.size) {
            map[i] = random.nextInt(2)

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }

        repeat(repeats) {
            var x1 = 0
            var y1 = 0
            for (i in 0 until map.size) {
                val c = numNeighbours(x1,y1)

                if (map[x1 + y1 * mapWidth] == 0 && c < 4) {
                    map[x1 + y1 * mapWidth] = 1
                }
                else if (map[x1 + y1 * mapWidth] == 1 && c >= 5) {
                    map[x1 + y1 * mapWidth] = 0
                }

                x1 += 1
                if (x1 >= mapWidth) {
                    x1 = 0
                    y1 += 1
                }
            }
        }
    }

    private fun numNeighbours(x: Int, y: Int): Int {
        var count = 0
        if (x > 0) {
            if (map[(x-1) + y * mapWidth] == 0) {
                count += 1
            }

            if (y > 0) {
                if (map[(x-1) + (y-1) * mapWidth] == 0) {
                    count += 1
                }
            }

            if (y < mapHeight - 1) {
                if (map[(x-1) + (y+1) * mapWidth] == 0) {
                    count += 1
                }
            }
        }

        if (x < mapWidth - 1) {
            if (map[(x+1) + y * mapWidth] == 0) {
                count += 1
            }

            if (y > 0) {
                if (map[(x+1) + (y-1) * mapWidth] == 0) {
                    count += 1
                }
            }

            if (y < mapHeight - 1) {
                if (map[(x+1) + (y+1) * mapWidth] == 0) {
                    count += 1
                }
            }
        }

        if (y > 0) {
            if (map[x + (y-1) * mapWidth] == 0) {
                count += 1
            }
        }

        if (y < mapHeight - 1) {
            if (map[x + (y+1) * mapWidth] == 0) {
                count += 1
            }
        }

        return count
    }

    private fun buildRooms() {
        val mapCopy = map.copyOf()

        var x = 0
        var y = 0
        for (i in 0 until map.size) {
            // Floor tile found - flood fill the room
            if (mapCopy[i] == 0) {
                val area = Vector4i(Int.MAX_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
                val tiles = floodSearchRoom(x,y,area,mapCopy,ArrayList())
                val room = Room(tiles, area, RoomType.values()[Random().nextInt(3)])
                rooms.add(room)
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }

        removeTinyRooms()
        findNearestNeighbourOfRooms()
        connectRooms()
    }

    private fun findNearestNeighbourOfRooms() {
        for (room in rooms) {
            while (room.neighbourRooms.size < 3) {
                var nearestRoom: Room? = null
                var shortestDist = Double.MAX_VALUE
                for (otherRoom in rooms) {
                    if (otherRoom == room || room.neighbourRooms.contains(otherRoom)) {
                        continue
                    }

                    val dist = otherRoom.area.distance(room.area)
                    if (dist < shortestDist) {
                        shortestDist = dist
                        nearestRoom = otherRoom
                    }
                }

                if (nearestRoom != null) {
                    room.neighbourRooms.add(nearestRoom)
                    nearestRoom.neighbourRooms.add(room)
                }
                else {
                    throw IllegalStateException("Unable to find a neighbouring room!")
                }
            }
        }
    }

    private fun removeTinyRooms() {
        var k = 0
        for (i in 0 until rooms.size) {
            if (k >= rooms.size) {
                break
            }
            if (rooms[k].tiles.size < 50) {
                for (tiles in rooms[k].tiles) {
                    map[tiles.x + tiles.y * mapWidth] = 1
                }
                rooms.removeAt(k)
                if (k > 0) {
                    k -= 1
                }
            }

            k += 1
        }
    }

    private fun connectRooms() {

        for (room in rooms) {
            for (neighbour in room.neighbourRooms) {
                var shortestLength = Int.MAX_VALUE
                var firstTile = Vector2i(0,0)
                var secondTile = Vector2i(0,0)

                // Find the tile closest to the neighbour rooms center as a starting point
                var ln = Int.MAX_VALUE
                for (tile in room.tiles) {
                    val dx = ((neighbour.area.x+neighbour.area.z)/2) - tile.x
                    val dy = ((neighbour.area.y+neighbour.area.w)/2) - tile.y
                    val k = dx*dx + dy*dy
                    if (k < ln) {
                        firstTile = tile
                        ln = k
                    }
                }

                // Find the tile in the neighbouring room which is closest to the selected
                // tile
                for (otherTile in neighbour.tiles) {
                    val dx = otherTile.x - firstTile.x
                    val dy = otherTile.y - firstTile.y
                    val d = dx * dx + dy * dy

                    if (d < shortestLength) {
                        shortestLength = d
                        secondTile = otherTile
                    }
                }

                val dx = (secondTile.x - firstTile.x).sign
                val dy = (secondTile.y - firstTile.y).sign

                var x = firstTile.x
                var y = firstTile.y

                // Create a tunnel between the two rooms, but stop as soon as we hit a collision
                // This happens in cases when there's another room in between the two neighbouring
                // rooms
                while (true) {
                    var traversalDone = 0
                    var collisions = 0

                    if (map[x + y * mapWidth] == 1) {
                        map[x + y * mapWidth] = 0
                        room.tiles.add(Vector2i(x,y))
                    }
                    else {
                        collisions++
                    }

                    if (x > 0) {
                        if (map[(x - 1) + y * mapWidth] == 1) {
                            map[(x - 1) + y * mapWidth] = 0
                            room.tiles.add(Vector2i(x - 1, y))
                        }
                        else {
                            collisions++
                        }
                    }

                    if (x < mapWidth - 1) {
                        if (map[(x + 1) + y * mapWidth] == 1) {
                            map[(x + 1) + y * mapWidth] = 0
                            room.tiles.add(Vector2i(x + 1, y))
                        }
                        else {
                            collisions++
                        }
                    }

                    if (y > 0) {
                        if (map[x + (y - 1) * mapWidth] == 1) {
                            map[x + (y - 1) * mapWidth] = 0
                            room.tiles.add(Vector2i(x, y - 1))
                        }
                        else {
                            collisions++
                        }
                    }

                    if (y < mapHeight - 1) {
                        if (map[x + (y + 1) * mapWidth] == 1) {
                            map[x + (y + 1) * mapWidth] = 0
                            room.tiles.add(Vector2i(x, y + 1))
                        }
                        else {
                            collisions++
                        }
                    }

                    if (x != secondTile.x) {
                        x += dx
                    }
                    else {
                        traversalDone += 1
                    }

                    if (y != secondTile.y) {
                        y += dy
                    }
                    else {
                        traversalDone += 1
                    }

                    if (traversalDone == 2 || collisions > 3) {
                        break
                    }
                }
            }
        }
    }

    private fun floodSearchRoom(x: Int, y: Int, area: Vector4i, mapCopy: IntArray, tiles: MutableList<Vector2i>): MutableList<Vector2i> {
        if (x < area.x) {
            area.x = x
        }
        if (y < area.y) {
            area.y = y
        }
        if (x > area.z) {
            area.z = x
        }
        if (y > area.w) {
            area.w = y
        }

        tiles.add(Vector2i(x, y))
        mapCopy[x + y*mapWidth] = 1

        if (x > 0) {
            if (mapCopy[(x-1) + y*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x-1, y, area, mapCopy, ArrayList()))
            }

            if (y > 0) {
                if (mapCopy[(x-1) + (y-1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x-1, y-1, area, mapCopy, ArrayList()))
                }
            }

            if (y < mapHeight - 1) {
                if (mapCopy[(x-1) + (y+1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x-1, y+1, area, mapCopy, ArrayList()))
                }
            }
        }

        if (y > 0) {
            if (mapCopy[x + (y-1)*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x, y-1, area, mapCopy, ArrayList()))
            }
        }

        if (x < mapWidth - 1) {
            if (mapCopy[(x+1) + y*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x+1, y, area, mapCopy, ArrayList()))
            }

            if (y > 0) {
                if (mapCopy[(x+1) + (y-1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x+1, y-1, area, mapCopy, ArrayList()))
                }
            }

            if (y < mapHeight - 1) {
                if (mapCopy[(x+1) + (y+1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x+1, y+1, area, mapCopy, ArrayList()))
                }
            }
        }

        if (y < mapHeight - 1) {
            if (mapCopy[x + (y+1)*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x, y+1, area, mapCopy, ArrayList()))
            }
        }

        return tiles
    }

    private fun generateEnemies(scene: Scene) {
        val random = Random()
        for (i in 0 until 50) {
            val kracGuy = Krac()
            enemySystem.newEntity(kracGuy)
                    .attachTransformComponent()
                    .attachSpriteComponent(enemyMaterial)
                    .build(scene)

            val room = rooms[random.nextInt(rooms.size)]
            val p = room.tiles[random.nextInt(room.tiles.size)]
            kracGuy.setPosition(enemySystem, Vector2i(p.x*64, p.y*64))
            enemies.add(kracGuy)
        }
    }

    private fun saveMapAsImage(filename: String) {
        val image = BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_3BYTE_BGR)

        for (r in rooms) {
            val color = Math.min(0xffffff, Random().nextInt(0xffffff) + 128)
            for (t in r.tiles) {
                image.setRGB(t.x, t.y, color)
            }
        }

        ImageIO.write(image, "png", File("./data/" + filename))
    }
}
