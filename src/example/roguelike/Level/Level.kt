package example.roguelike.Level

import org.joml.Vector2i
import org.joml.Vector3f
import rain.api.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

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

    private var mapBackIndices = Array(0){ Tilemap.TileIndex(0,0)}
    private var mapFrontIndices = Array(0){Tilemap.TileIndex(0,0)}
    private var rooms = ArrayList<Room>()

    fun create(resourceFactory: ResourceFactory, mapWidth: Int, mapHeight: Int, width: Int, height: Int) {
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
    }

    fun switchCell(resourceFactory: ResourceFactory, cellX: Int, cellY: Int) {
        val backIndices = Array(width*height){ Tilemap.TileIndex(0,1)}
        val frontIndices = Array(width*height){ Tilemap.TileIndex(0,1)}
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
            backTilemap.create(resourceFactory, material, width, height, 32.0f, 32.0f, backIndices)
            frontTilemap.create(resourceFactory, material, width, height, 32.0f, 32.0f, frontIndices)
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)

            backTilemap.getTransform().position.set(0.0f, 0.0f, 1.0f)
            frontTilemap.getTransform().position.set(0.0f, 0.0f, 3.0f)
            firstBuild = false
        }
        else {
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
        }
    }

    fun build(resourceFactory: ResourceFactory, seed: Long) {
        generate(seed, 5)

        buildRooms()
        var x = 0
        var y = 0

        mapBackIndices = Array(mapWidth*mapHeight){ Tilemap.TileIndex(0,1)}
        mapFrontIndices = Array(mapWidth*mapHeight){ Tilemap.TileIndex(0,1)}
        for (i in 0 until mapWidth * mapHeight) {
            if (map[i] == 1) {
                mapBackIndices[i] = Tilemap.TileIndex(1, 1)
                if (y > 0 && y < mapHeight - 1) {
                    if (map[x + (y-1)*mapWidth] == 0 &&
                        map[x + (y+1)*mapWidth] == 0) {
                        mapBackIndices[i] = Tilemap.TileIndex(0,0)
                        map[i] = 0
                    }
                    else if(map[x + (y+1)*mapWidth] == 1) {
                        mapFrontIndices[i] = Tilemap.TileIndex(3,1)
                        if (y < mapHeight - 2) {
                            if (map[x + (y+2)*mapWidth] == 1) {
                                mapFrontIndices[i] = Tilemap.TileIndex(2,1)
                            }
                        }
                    }
                }
                else if(y == 0) {
                    if (map[x + (y+1)*mapWidth] == 1) {
                        mapFrontIndices[i] = Tilemap.TileIndex(2, 1)
                    }
                }
            }
            else {
                mapBackIndices[i] = Tilemap.TileIndex(0, 0)
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }

        saveMapAsImage()
        switchCell(resourceFactory, 0, 0)
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
                val tiles = floodSearchRoom(x,y,mapCopy,ArrayList())
                val room = Room(tiles, RoomType.CAVE)
                rooms.add(room)
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }

        connectRooms()
    }

    private fun connectRooms() {
        for (i in 0 until 3) {
            var rindex = 0
            for (room in rooms) {
                // Find the room that is the nearest to this room
                var shortestLength = Int.MAX_VALUE
                var firstTile = Vector2i()
                var secondTile = Vector2i()
                var nRoom: Room = room
                var nRoomIndex = 0
                var index = 0
                for (otherRoom in rooms) {
                    if (index != rindex && !room.connectedRooms.contains(index) && !otherRoom.connectedRooms.contains(rindex)) {
                        for (tile in room.tiles) {
                            for (otherTile in otherRoom.tiles) {
                                val dx = otherTile.x - tile.x
                                val dy = otherTile.y - tile.y
                                val d = dx * dx + dy * dy
                                if (d < shortestLength) {
                                    shortestLength = d
                                    firstTile = tile
                                    secondTile = otherTile
                                    nRoom = room
                                    nRoomIndex = index
                                }
                            }
                        }
                    }
                    index++
                }

                room.connectedRooms.add(nRoomIndex)
                nRoom.connectedRooms.add(rindex)
                rindex++

                val dx = secondTile.x - firstTile.x
                val dy = secondTile.y - firstTile.y

                if (dx == 0 && dy == 0) {
                    println("Ugh")
                }

                var x = firstTile.x
                var y = firstTile.y
                while (true) {
                    map[x + y * mapWidth] = 0

                    if (x > 0) {
                        map[(x - 1) + y * mapWidth] = 0
                    }

                    if (x < mapWidth - 1) {
                        map[(x + 1) + y * mapWidth] = 0
                    }

                    if (y > 0) {
                        map[x + (y - 1) * mapWidth] = 0
                    }

                    if (y < mapHeight - 1) {
                        map[x + (y + 1) * mapWidth] = 0
                    }

                    if (x != secondTile.x) {
                        if (dx > 0) {
                            x += 1
                        } else {
                            x -= 1
                        }
                    }

                    if (y != secondTile.y) {
                        if (dy > 0) {
                            y += 1
                        } else {
                            y -= 1
                        }
                    }

                    if (x == secondTile.x && y == secondTile.y) {
                        break
                    }
                }
            }
        }
    }

    private fun floodSearchRoom(x: Int, y: Int, mapCopy: IntArray, tiles: MutableList<Vector2i>): List<Vector2i> {
        tiles.add(Vector2i(x, y))
        mapCopy[x + y*mapWidth] = 1

        if (x > 0) {
            if (mapCopy[(x-1) + y*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x-1, y, mapCopy, ArrayList()))
            }
        }

        if (y > 0) {
            if (mapCopy[x + (y-1)*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x, y-1, mapCopy, ArrayList()))
            }
        }

        if (x < mapWidth - 1) {
            if (mapCopy[(x+1) + y*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x+1, y, mapCopy, ArrayList()))
            }
        }

        if (y < mapHeight - 1) {
            if (mapCopy[x + (y+1)*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x, y+1, mapCopy, ArrayList()))
            }
        }

        return tiles
    }

    private fun saveMapAsImage() {
        val image = BufferedImage(mapWidth, mapHeight, BufferedImage.TYPE_3BYTE_BGR)
        var x = 0
        var y = 0

        for (i in map) {
            if (i == 1) {
                image.setRGB(x,y,0xa0938e)
            }
            else if (i == 0) {
                image.setRGB(x,y,0x5e3643)
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
            }
        }

        for (r in rooms) {
            val color = Random().nextInt(0xffffff)
            for (t in r.tiles) {
                image.setRGB(t.x, t.y, color)
            }
        }

        ImageIO.write(image, "png", File("./data/map.png"))
    }
}
