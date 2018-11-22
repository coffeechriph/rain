package example.roguelike.Level

import com.badlogic.gdx.physics.box2d.BodyDef
import example.roguelike.Entity.*
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4i
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.scene.*
import java.util.*
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
    var detailTilemap = Tilemap()
        private set
    var minimapTilemap = Tilemap()

    private lateinit var material: Material
    private lateinit var itemMaterial: Material
    private lateinit var texture: Texture2d
    private var firstBuild = true

    private var mapBackIndices = Array(0){ TileIndex(0, 0) }
    private var mapFrontIndices = Array(0){ TileIndex(0, 0) }
    private var mapDetailIndices = Array(0) { TileIndexNone }
    private var rooms = ArrayList<Room>()
    private var enemies = ArrayList<Enemy>()
    private lateinit var random: Random
    private lateinit var enemySystem: EntitySystem<Enemy>
    private lateinit var enemyMaterial: Material
    private lateinit var collisionSystem: EntitySystem<Entity>
    private lateinit var containerSystem: EntitySystem<Container>
    private lateinit var levelItemSystem: EntitySystem<Item>
    private var containers = ArrayList<Container>()
    private lateinit var navMesh: NavMesh
    var startPosition = Vector2i()
    var exitPosition = Vector2i()

    fun update(player: Player) {
        for (enemy in enemies) {
            enemy.sprite.visible = enemy.health > 0 && enemy.cellX == player.cellX && enemy.cellY == player.cellY
            enemy.healthBar.sprite.visible = enemySystem.findSpriteComponent(enemy.getId())!!.visible
            enemy.collider.setActive(enemy.sprite.visible)

            if (!enemy.sprite.visible) {
                continue
            }

            enemy.healthBar.transform.sx = enemy.health / 2.0f

            if (!enemy.traversing) {
                enemy.collider.setVelocity(0.0f,0.0f)
                if (enemy.transform.x < player.transform.x - 16.0f || enemy.transform.x > player.transform.x + 16.0f
                && enemy.transform.y < player.transform.y - 16.0f || enemy.transform.y > player.transform.y + 16.0f) {
                    val worldX = enemy.transform.x / 64 + (enemy.cellX * width)
                    val worldY = enemy.transform.y / 64 + (enemy.cellY * height)
                    val px = player.transform.x / 64 + (player.cellX * width)
                    val py = player.transform.y / 64 + (player.cellY * height)

                    val path = navMesh.findPath(Vector2i(worldX.toInt(), worldY.toInt()), Vector2i(px.toInt(), py.toInt()))
                    enemy.path = path
                    enemy.pathIndex = 1
                    enemy.traversing = true
                    enemy.traverseSleep = System.currentTimeMillis()

                    val dx = player.transform.x - enemy.transform.x
                    val dy = player.transform.y - enemy.transform.y
                    enemy.lastPlayerAngle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                }
            }
            else if (enemy.path.size > 0 && enemy.pathIndex < enemy.path.size) {
                val ax = player.transform.x - enemy.transform.x
                val ay = player.transform.y - enemy.transform.y
                val currAngle = Math.atan2(ay.toDouble(), ax.toDouble()).toFloat()
                if (Math.abs(enemy.lastPlayerAngle-currAngle) >= Math.PI*0.3f) {
                    enemy.traversing = false
                }
                else {
                    val target = Vector2i(enemy.path[enemy.pathIndex])
                    target.mul(64)
                    target.x %= 1280
                    target.y %= 752
                    val dx = target.x - enemy.transform.x
                    val dy = target.y - enemy.transform.y
                    val ln = Math.sqrt((dx * dx + dy * dy).toDouble())
                    if (ln <= 0) {
                        enemy.collider.setVelocity(0.0f, 0.0f)
                        enemy.pathIndex += 1
                        if (enemy.pathIndex >= enemy.path.size) {
                            enemy.traversing = false
                        }
                    } else {
                        val vx = dx / ln
                        val vy = dy / ln

                        enemy.collider.setVelocity(vx.toFloat() * 120.0f, vy.toFloat() * 120.0f)
                        if (enemy.transform.x >= target.x - 16 && enemy.transform.x <= target.x + 16 &&
                                enemy.transform.y >= target.y - 16 && enemy.transform.y <= target.y + 16 ||
                                enemy.transform.x >= player.transform.x - 16.0f && enemy.transform.x <= player.transform.x + 16.0f
                                && enemy.transform.y >= player.transform.y - 16.0f && enemy.transform.y <= player.transform.y + 16.0f) {
                            enemy.pathIndex += 1
                            if (enemy.pathIndex >= enemy.path.size) {
                                enemy.traversing = false
                            }
                        } else if (System.currentTimeMillis() - enemy.traverseSleep >= 500 && enemy.pathIndex >= 5) {
                            enemy.traversing = false
                        }
                    }
                }
            }
            else {
                enemy.traversing = false
            }
        }

        for (container in containers) {
            container.sprite.visible = container.cellX == player.cellX && container.cellY == player.cellY
            container.collider.setActive(container.sprite.visible)

            if (container.open && !container.looted) {
                container.looted = true

                for (i in 0 until random.nextInt(5) + 1) {
                    val combination = ITEM_COMBINATIONS[random.nextInt(ITEM_COMBINATIONS.size)]
                    val name = combination.second[random.nextInt(combination.second.size)]

                    var quality = random.nextFloat() * random.nextFloat()
                    quality *= 100
                    quality += random.nextFloat() * (player.currentLevel.toFloat()) * 0.02f
                    var qualityName = "None"
                    var qualityIndex = 1.0f
                    for (q in ITEM_QUALITIES) {
                        if (quality.toInt() in q.first) {
                            qualityName = q.second
                            break
                        }

                        qualityIndex += 1
                    }

                    val finalQuality = (qualityIndex*qualityIndex + qualityIndex).toInt()

                    val item = Item(combination.first, "$qualityName $name", random.nextInt(finalQuality)+1, random.nextInt(finalQuality)+1,
                            random.nextInt(finalQuality)+1,random.nextInt(finalQuality)+1)
                    levelItemSystem.newEntity(item)
                            .attachTransformComponent()
                            .attachSpriteComponent(itemMaterial)
                            .attachBoxColliderComponent(64.0f, 64.0f)
                            .build()
                    item.setPosition(levelItemSystem, Vector2i(container.collider.getPosition().x.toInt(), container.collider.getPosition().y.toInt()))
                    item.cellX = player.cellX
                    item.cellY = player.cellY
                    item.transform.sx = 64.0f
                    item.transform.sy = 64.0f
                    item.sprite.textureTileOffset.x = 3
                    item.sprite.textureTileOffset.y = 4 + random.nextInt(3)

                    item.collider.setDamping(300.0f)
                    item.collider.setFriction(0.0f)
                    item.collider.setDensity(0.0f)
                }
            }
        }

        for (item in levelItemSystem.getEntityList()) {
            item!!.sprite.visible = item.cellX == player.cellX && item.cellY == player.cellY
            item.collider.setActive(item.sprite.visible)

            if (item.pickedUp) {
                item.sprite.visible = false
                item.collider.setActive(false)
            }
        }
    }

    fun getFirstTilePos(): Vector2i {
        return Vector2i(startPosition.x * 64, startPosition.y * 64)
    }

    fun create(resourceFactory: ResourceFactory, scene: Scene, mapWidth: Int, mapHeight: Int, width: Int, height: Int) {
        maxCellX = mapWidth / width
        maxCellY = mapHeight / height
        texture = resourceFactory.loadTexture2d("./data/textures/tiles.png", TextureFilter.NEAREST)
        texture.setTiledTexture(16,16)
        material = resourceFactory.createMaterial("./data/shaders/tilemap.vert.spv", "./data/shaders/basic.frag.spv", texture, Vector3f(1.0f,1.0f, 1.0f))
        itemMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", texture, Vector3f(1.0f, 1.0f, 1.0f))
        this.mapWidth = mapWidth
        this.mapHeight = mapHeight
        this.width = width
        this.height = height
        map = IntArray(mapWidth*mapHeight)
        navMesh = NavMesh(mapWidth, mapHeight)

        val enemyTexture = resourceFactory.loadTexture2d("./data/textures/krac2.0.png", TextureFilter.NEAREST)
        enemyTexture.setTiledTexture(16,16)
        enemyMaterial = resourceFactory.createMaterial("./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", enemyTexture, Vector3f(1.0f, 1.0f, 1.0f))
        enemySystem = EntitySystem(scene)
        scene.addSystem(enemySystem)

        collisionSystem = EntitySystem(scene)
        scene.addSystem(collisionSystem)

        containerSystem = EntitySystem(scene)
        scene.addSystem(containerSystem)

        levelItemSystem = EntitySystem(scene)
        scene.addSystem(levelItemSystem)
    }

    fun switchCell(resourceFactory: ResourceFactory, cellX: Int, cellY: Int) {
        val backIndices = Array(width*height){ TileIndexNone }
        val frontIndices = Array(width*height){ TileIndexNone }
        var detailIndices = Array(width*height){ TileIndexNone }
        var sx = cellX * width
        var sy = cellY * height

        var cx = 0.0f
        var cy = 0.0f
        collisionSystem.clear()
        for (i in 0 until width*height) {
            if (sx + sy*mapWidth >= map.size) {
                break
            }

            backIndices[i] = mapBackIndices[sx + sy*mapWidth]
            frontIndices[i] = mapFrontIndices[sx + sy*mapWidth]
            detailIndices[i] = mapDetailIndices[sx + sy*mapWidth]

            if (map[sx + sy*mapWidth] == 1) {
                val e = Entity()
                collisionSystem.newEntity(e)
                        .attachTransformComponent()
                        .attachBoxColliderComponent(64.0f, 64.0f, BodyDef.BodyType.StaticBody)
                        .build()
                val tr = collisionSystem.findTransformComponent(e.getId())
                val cl = collisionSystem.findColliderComponent(e.getId())!!
                cl.setPosition(cx + 32.0f, cy + 32.0f)
                cl.setFriction(0.5f)
                tr!!.sx = 64.0f
                tr.sy = 64.0f
                tr.z = 13.0f
            }

            sx += 1
            if (sx >= cellX * width + width) {
                sx = cellX * width
                sy += 1
            }

            cx += 64.0f
            if (cx >= width * 64.0f) {
                cx = 0.0f
                cy += 64.0f
            }
        }

        if (firstBuild) {
            backTilemap.create(resourceFactory, material, width, height, 64.0f, 64.0f, backIndices)
            frontTilemap.create(resourceFactory, material, width, height, 64.0f, 64.0f, frontIndices)
            detailTilemap.create(resourceFactory, material, width, height, 64.0f, 64.0f, detailIndices)
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
            detailTilemap.update(detailIndices)
            backTilemap.transform.setPosition(0.0f, 0.0f, 1.0f)
            detailTilemap.transform.setPosition(0.0f, 0.0f, 1.1f)
            frontTilemap.transform.setPosition(0.0f, 0.0f, 10.0f)

            minimapTilemap.transform.setPosition(0.0f, 0.0f, 13.0f)
            firstBuild = false
        }
        else {
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
            detailTilemap.update(detailIndices)
        }
    }

    fun build(resourceFactory: ResourceFactory, seed: Long, healthBarSystem: EntitySystem<HealthBar>, healthBarMaterial: Material) {
        random = Random(seed)
        generate(7)
        addWallBlockersAtEdges()
        buildRooms()

        for (i in 0 until map.size) {
            navMesh.map[i] = (map[i] * 127).toByte()
        }

        mapBackIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapFrontIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapDetailIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        populateTilemap()

        // Set position of start and exit
        val startRoom = rooms[0]
        val endRoom = rooms[0]

        startPosition = startRoom.findNoneEdgeTile(random)
        exitPosition = endRoom.findNoneEdgeTile(random)

        mapBackIndices[exitPosition.x + exitPosition.y * mapWidth] = TileIndex(2, 2)

        generateEnemies(healthBarMaterial, healthBarSystem)
        generateContainers()

        val minimapIndices = Array(mapWidth*mapHeight){ TileIndex(2, 1) }
        for (i in 0 until rooms.size) {
            for (j in 0 until rooms[i].tiles.size) {
                minimapIndices[rooms[i].tiles[j].x + rooms[i].tiles[j].y * mapWidth] = TileIndex(7, rooms[i].type.ordinal)
            }
        }
        minimapIndices[exitPosition.x + exitPosition.y * mapWidth] = TileIndex(2, 2)
        minimapTilemap.create(resourceFactory, material, mapWidth, mapHeight, 2.0f, 2.0f, minimapIndices)
        minimapTilemap.update(minimapIndices)
    }

    private fun addWallBlockersAtEdges() {
        // Find tiles at the edge of a cell
        // If they are solid we want to extend them into the neighbouring cell
        // This is to remove the buggy cell change which causes wall stucks and spasm
        var x = 0
        var y = 0
        for (i in 0 until maxCellX * maxCellY) {
            if (y < mapHeight - 1) {
                for (k in 0 until width) {
                    if (map[(x + k) + y * mapWidth] == 1) {
                        map[(x + k) + (y + 1) * mapWidth] = 1
                    }
                }
            }

            if (y > 0) {
                for (k in 0 until width) {
                    if (map[(x + k) + y * mapWidth] == 1) {
                        map[(x + k) + (y - 1) * mapWidth] = 1
                    }
                }
            }

            if (x < mapWidth - 1 && y + width < mapHeight) {
                for (k in 0 until width) {
                    if (map[x + (y + k) * mapWidth] == 1) {
                        map[(x + 1) + (y + k) * mapWidth] = 1
                    }
                }
            }

            if (x > 0 && y + width < mapHeight) {
                for (k in 0 until width) {
                    if (map[x + (y + k) * mapWidth] == 1) {
                        map[(x - 1) + (y + k) * mapWidth] = 1
                    }
                }
            }

            x += width
            if (x >= mapWidth) {
                x = 0
                y += height
            }
        }
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
                                mapFrontIndices[tile.x + (tile.y - 2) * mapWidth] = TileIndex(3, tileY)
                            }
                            else {
                                mapFrontIndices[tile.x + (tile.y - 2) * mapWidth] = TileIndex(3, tileY)
                                map[tile.x + (tile.y - 2) * mapWidth] = 1
                            }
                        }
                    }
                }

                // Add black tiles at the edges where there's atleast 3 walls below a floor tile
                // The wall closest to the top floor tile will be populated with a black tile
                if (tile.y < mapHeight - 3) {
                    if (map[tile.x + (tile.y+1) * mapWidth] == 1 &&
                        map[tile.x + (tile.y+2) * mapWidth] == 1 &&
                        map[tile.x + (tile.y+3) * mapWidth] == 1) {
                        mapFrontIndices[tile.x + (tile.y+1) * mapWidth] = TileIndex(2, 1)
                        map[tile.x + (tile.y + 1) * mapWidth] = 1
                    }
                }

                val r = random.nextInt(40)
                if (r == 1){
                    mapDetailIndices[tile.x + (tile.y+1) * mapWidth] = TileIndex(5, 1)
                }
                else if (r == 2) {
                    mapDetailIndices[tile.x + (tile.y+1) * mapWidth] = TileIndex(5, 2)
                }
            }
        }

        // Remove tiles that are now solid
        for (room in rooms) {
            val tilesToRemove = ArrayList<Vector2i>()
            for (tile in room.tiles) {
                val index = tile.x + tile.y * mapWidth
                if (map[index] == 1) {
                    tilesToRemove.add(tile)
                }
            }

            for (tile in tilesToRemove) {
                room.tiles.remove(tile)
            }
        }
    }

    private fun generate(repeats: Int) {
        var x = 0
        var y = 0
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

        x = 0
        y = 0
        for (i in 0 until map.size) {
            if (x == 0 || x == mapWidth-1 || y == 0 || y == mapHeight-1) {
                map[x + y*mapWidth] = 1
            }

            x += 1
            if (x >= mapWidth) {
                x = 0
                y += 1
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
        rooms.clear()
        val mapCopy = map.copyOf()

        var x = 0
        var y = 0
        for (i in 0 until map.size) {
            // Floor tile found - flood fill the room
            if (mapCopy[i] == 0) {
                val area = Vector4i(Int.MAX_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MIN_VALUE)
                val tiles = floodSearchRoom(x,y,area,mapCopy,ArrayList())
                val room = Room(tiles, area, RoomType.values()[random.nextInt(3)])
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
                    break
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

                        if (y > 0) {
                            if (map[(x - 1) + (y - 1) * mapWidth] == 1) {
                                map[(x - 1) + (y - 1) * mapWidth] = 0
                                room.tiles.add(Vector2i(x - 1, y - 1))
                            }
                            else {
                                collisions++
                            }
                        }

                        if (y < mapHeight - 1) {
                            if (map[(x - 1) + (y + 1) * mapWidth] == 1) {
                                map[(x - 1) + (y + 1) * mapWidth] = 0
                                room.tiles.add(Vector2i(x - 1, y + 1))
                            }
                            else {
                                collisions++
                            }
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


                        if (y > 0) {
                            if (map[(x + 1) + (y - 1) * mapWidth] == 1) {
                                map[(x + 1) + (y - 1) * mapWidth] = 0
                                room.tiles.add(Vector2i(x + 1, y - 1))
                            }
                            else {
                                collisions++
                            }
                        }

                        if (y < mapHeight - 1) {
                            if (map[(x + 1) + (y + 1) * mapWidth] == 1) {
                                map[(x + 1) + (y + 1) * mapWidth] = 0
                                room.tiles.add(Vector2i(x + 1, y + 1))
                            }
                            else {
                                collisions++
                            }
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

                    if (traversalDone == 2 || collisions > 7) {
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

    private fun generateEnemies(healthBarMaterial: Material, healthBarSystem: EntitySystem<HealthBar>) {
        enemies.clear()
        enemySystem.clear()
        healthBarSystem.clear()

        for (i in 0 until random.nextInt(50) + 10) {
            val kracGuy = Krac()
            enemySystem.newEntity(kracGuy)
                    .attachTransformComponent()
                    .attachSpriteComponent(enemyMaterial)
                    .attachAnimatorComponent()
                    .attachBoxColliderComponent(width = 12.0f, height = 16.0f)
                    .build()

            val et = enemySystem.findTransformComponent(kracGuy.getId())!!
            kracGuy.collider.setDamping(100.0f)
            kracGuy.collider.setFriction(0.0f)
            kracGuy.healthBar.parentTransform = et

            healthBarSystem.newEntity(kracGuy.healthBar)
                    .attachTransformComponent()
                    .attachSpriteComponent(healthBarMaterial)
                    .build()

            kracGuy.healthBar.transform.sx = 60.0f
            kracGuy.healthBar.transform.sy = 7.0f

            val room = rooms[random.nextInt(rooms.size)]
            val p = room.findNoneEdgeTile(random)
            kracGuy.setPosition(Vector2i(p.x*64, p.y*64))
            enemies.add(kracGuy)
        }
    }

    private fun generateContainers() {
        containers.clear()
        containerSystem.clear()

        for (i in 0 until random.nextInt(50) + 20) {
            val container = Container()
            containerSystem.newEntity(container)
                    .attachTransformComponent()
                    .attachSpriteComponent(itemMaterial)
                    .attachBoxColliderComponent(32.0f, 32.0f, BodyDef.BodyType.StaticBody)
                    .build()
            val room = rooms[random.nextInt(rooms.size)]
            val tile = room.findNoneEdgeTile(random)

            container.setPosition(Vector2i(tile.x*64, tile.y*64))
            container.collider.setDamping(100.0f)
            container.collider.setDensity(1000.0f)
            container.collider.setFriction(1.0f)
            containers.add(container)
        }
    }
}
