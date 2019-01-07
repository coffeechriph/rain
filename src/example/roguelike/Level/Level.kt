package example.roguelike.Level

import com.badlogic.gdx.physics.box2d.BodyDef
import example.roguelike.Entity.*
import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBImageWrite
import rain.api.entity.DirectionType
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Transform
import rain.api.gfx.*
import rain.api.scene.*
import rain.vulkan.VertexAttribute
import java.lang.Math
import kotlin.math.sign

class Level(val player: Player, val resourceFactory: ResourceFactory) {
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
    private lateinit var lightMap: VertexBuffer
    private lateinit var lightVertices: FloatArray
    private lateinit var lightValues: Array<Vector4f>
    private lateinit var lightMapMaterial: Material

    private lateinit var tilemapMaterial: Material
    private lateinit var itemMaterial: Material
    private lateinit var texture: Texture2d
    private lateinit var torchTexture: Texture2d
    private lateinit var torchMaterial: Material
    private lateinit var torchSystem: EntitySystem<LightSource>
    private var firstBuild = true

    private var mapBackIndices = Array(0){ TileIndex(0, 0) }
    private var mapFrontIndices = Array(0){ TileIndex(0, 0) }
    private var mapDetailIndices = Array(0) { TileIndexNone }
    private var rooms = ArrayList<Room>()
    private lateinit var random: Random
    private lateinit var enemySystem: EntitySystem<Enemy>
    private lateinit var enemyTexture: Texture2d
    private lateinit var enemyMaterial: Material
    private lateinit var collisionSystem: EntitySystem<Entity>
    private lateinit var containerSystem: EntitySystem<Container>
    private lateinit var levelItemSystem: EntitySystem<Item>
    private lateinit var xpBallSystem: EntitySystem<XpBall>
    private lateinit var navMesh: NavMesh
    var startPosition = Vector2i()
    var exitPosition = Vector2i()

    private var delayLightUpdate = 0

    private val activeEnemies = ArrayList<Enemy>()
    private val activeContainers = ArrayList<Container>()
    private val activeLightSources = ArrayList<LightSource>()

    fun update(deltaTime: Float) {
        if (delayLightUpdate == 0) {
            generateLightMap()
            delayLightUpdate = 2
        }
        else {
            delayLightUpdate -= 1
        }

        // Add navmesh blockers at the location of enemies
        for (enemy in activeEnemies) {
            if (enemy.health <= 0) {
                continue
            }

            if (enemy.lastX > -1 && enemy.lastY > -1) {
                navMesh.map[enemy.lastX + enemy.lastY * width] = 0
            }

            var x = enemy.transform.x.toInt()/64
            var y = enemy.transform.y.toInt()/64
            if (x >= width) {
                x = width-1
            }
            if (y >= height) {
                y = height - 1
            }
            if (x < 0) {
                x = 0
            }
            if (y < 0) {
                y = 0
            }

            navMesh.map[x + y * width] = 127.toByte()
            enemy.lastX = x
            enemy.lastY = y
        }

        for (enemy in activeEnemies) {
            if (enemy.health <= 0) {
                if (enemy.sprite.visible) {
                    for (i in 0 until random.nextInt(5)+1) {
                        // Add xp balls to the world
                        val xpBall = XpBall(player)
                        xpBallSystem.newEntity(xpBall)
                                .attachTransformComponent()
                                .attachSpriteComponent(itemMaterial)
                                .build()

                        val px = (Math.sin(random.nextFloat()*Math.PI*2) * 32.0f).toInt()
                        val py = (Math.cos(random.nextFloat()*Math.PI*2) * 32.0f).toInt()
                        xpBall.setPosition(xpBallSystem, Vector2i(enemy.transform.x.toInt() + px, enemy.transform.y.toInt() + py))
                        xpBall.transform.sx = random.nextFloat() * 16.0f + 40.0f
                        xpBall.transform.sy = random.nextFloat() * 16.0f + 40.0f
                        xpBall.sprite.textureTileOffset.x = 5
                        xpBall.sprite.textureTileOffset.y = 6
                    }
                }

                enemy.sprite.visible = false
                enemy.collider.setActive(false)
                enemy.healthBar.sprite.visible = false
                continue
            }

            val dx = player.transform.x - enemy.transform.x
            val dy = player.transform.y - enemy.transform.y
            if (Math.sqrt((dx * dx + dy * dy).toDouble()) <= 64.0) {
                enemy.attack(player)
                player.inventory.updateHealthText()
            }

            enemy.healthBar.transform.sx = enemy.health / 2.0f

            if (!enemy.traversing) {
                enemy.collider.setVelocity(0.0f,0.0f)
                val kx = player.transform.x - enemy.transform.x
                val ky = player.transform.y - enemy.transform.y
                val dd = Math.sqrt((kx*kx+ky*ky).toDouble())
                if (dd > 64.0f) {
                    val worldX = enemy.transform.x / 64
                    val worldY = enemy.transform.y / 64
                    val px = (player.transform.x / 64).toInt()
                    val py = (player.transform.y / 64).toInt()

                    if (px < width && py < height && px >= 0 && py >= 0) {
                        val path = navMesh.findPath(Vector2i(worldX.toInt(), worldY.toInt()), Vector2i(px, py))
                        if (path[path.size-1].x == px && path[path.size-1].y == py) {
                            if (path.size > 2) {
                                enemy.path = path
                                enemy.pathIndex = 0
                                enemy.traversing = true
                                enemy.traverseSleep = System.currentTimeMillis()

                                val dx2 = player.transform.x - enemy.transform.x
                                val dy2 = player.transform.y - enemy.transform.y
                                enemy.lastPlayerAngle = Math.atan2(dy2.toDouble(), dx2.toDouble()).toFloat()
                            }
                        }
                    }
                }
            }
            else if (enemy.path.size > 0 && enemy.pathIndex < enemy.path.size) {
                // Move to first tile
                val target = Vector2i(enemy.path[enemy.pathIndex])
                target.x *= 64
                target.y *= 64
                val dx2 = (target.x + 32) - enemy.transform.x
                val dy2 = (target.y + 32) - enemy.transform.y
                val ln = Math.sqrt((dx2*dx2+dy2*dy2).toDouble());
                var vx: Float
                var vy: Float
                if (ln == 0.0) {
                    vx = dx2
                    vy = dy2
                }
                else {
                    vx = (dx2 / ln).toFloat()
                    vy = (dy2 / ln).toFloat()
                }

                if (enemy.pushBack == 0) {
                    enemy.pushBackImmune = false
                    enemy.collider.setVelocity(vx * enemy.walkingSpeedFactor * 100, vy * enemy.walkingSpeedFactor * 100)
                    if (vy > 0.0f) {
                        enemy.animator.setAnimation("walk_down")
                    }
                    else {
                        enemy.animator.setAnimation("walk_up")
                    }
                }
                else if (enemy.pushBack > 5) {
                    enemy.collider.applyLinearImpulseToCenter(enemy.pushDirection.x.toFloat() * 10, enemy.pushDirection.y.toFloat() * 10)
                    enemy.pushBack -= 1
                    enemy.animator.setAnimation("idle_down")
                }
                else {
                    enemy.pushBack -= 1
                    enemy.collider.setVelocity(0.0f, 0.0f)
                    enemy.animator.setAnimation("idle_down")
                }

                val pdx = player.transform.x - enemy.transform.x
                val pdy = player.transform.y - enemy.transform.y
                val pln = Math.sqrt((pdx*pdx+pdy*pdy).toDouble());
                if (pln <= 48.0f) {
                    enemy.traversing = false
                }
                else {
                    val dx3 = (target.x + 32) - enemy.transform.x
                    val dy3 = (target.y + 32) - enemy.transform.y
                    val ln2 = Math.sqrt((dx3 * dx3 + dy3 * dy3).toDouble());
                    if (ln2 <= 8.0f) {
                        enemy.pathIndex += 1
                        if (enemy.pathIndex >= enemy.path.size - 1) {
                            enemy.traversing = false
                        }
                    }
                    else if (enemy.pathIndex >= 3) {
                        enemy.traversing = false
                    }
                }
            }
            else {
                enemy.traversing = false
            }
        }

        for (container in activeContainers) {
            if (container.open && !container.looted) {
                container.looted = true
                val emitter = containerSystem.findBurstEmitterComponent(container.getId())!!
                emitter.fireSingleBurst()

                for (i in 0 until container.numItems) {
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

                    val item = Item(player, combination.first, "$qualityName $name", random.nextInt(finalQuality)+1, random.nextInt(finalQuality)+1,
                            random.nextInt(finalQuality)+1,random.nextInt(finalQuality)+1)
                    levelItemSystem.newEntity(item)
                            .attachTransformComponent()
                            .attachSpriteComponent(itemMaterial)
                            .build()
                    val angle = random.nextFloat()*Math.PI
                    val direction = Vector2f(Math.sin(angle).toFloat(), Math.cos(angle).toFloat())
                    direction.x *= 64.0f
                    direction.y *= 64.0f
                    item.setPosition(levelItemSystem, Vector2i(container.collider.getPosition().x.toInt()+direction.x.toInt(), container.collider.getPosition().y.toInt()+direction.y.toInt()))
                    item.cellX = player.cellX
                    item.cellY = player.cellY
                    item.transform.sx = 40.0f
                    item.transform.sy = 40.0f
                    item.transform.z = 2.0f
                    item.sprite.textureTileOffset.x = 3
                    item.sprite.textureTileOffset.y = 4 + random.nextInt(3)
                }
            }
        }
    }

    fun getFirstTilePos(): Vector2i {
        return Vector2i(startPosition.x * 64, startPosition.y * 64)
    }

    fun create(resourceFactory: ResourceFactory, scene: Scene, mapWidth: Int, mapHeight: Int, width: Int, height: Int) {
        firstBuild = true
        maxCellX = mapWidth / width
        maxCellY = mapHeight / height
        texture = resourceFactory.loadTexture2d("tilemapTexture","./data/textures/tiles.png", TextureFilter.NEAREST)
        texture.setTiledTexture(16,16)
        tilemapMaterial = resourceFactory.createMaterial("tilemapMaterial","./data/shaders/tilemap.vert.spv", "./data/shaders/basic.frag.spv", texture, enableBlend = false)
        itemMaterial = resourceFactory.createMaterial("itemMaterial", "./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", texture)
        this.mapWidth = mapWidth
        this.mapHeight = mapHeight
        this.width = width
        this.height = height
        map = IntArray(mapWidth*mapHeight)
        navMesh = NavMesh(width, height)
        navMesh.allowDiagonals = false

        enemyTexture = resourceFactory.loadTexture2d("enemyTexture","./data/textures/krac2.0.png", TextureFilter.NEAREST)
        enemyTexture.setTiledTexture(16,16)
        enemyMaterial = resourceFactory.createMaterial("enemyMaterial","./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", enemyTexture)
        enemySystem = scene.newSystem<Enemy>()

        collisionSystem = scene.newSystem<Entity>()
        containerSystem = scene.newSystem<Container>()
        levelItemSystem = scene.newSystem<Item>()
        xpBallSystem = scene.newSystem<XpBall>()

        torchTexture = resourceFactory.loadTexture2d("torch", "./data/textures/torch.png", TextureFilter.NEAREST)
        torchMaterial = resourceFactory.createMaterial("torchMaterial", "./data/shaders/basic.vert.spv", "./data/shaders/basic.frag.spv", torchTexture)
        torchSystem = scene.newSystem<LightSource>()

        lightVertices = FloatArray(width*height*6*6){0.0f}
        lightValues = Array(width*height){Vector4f()}
        lightMap = resourceFactory.createVertexBuffer(lightVertices, VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 2), VertexAttribute(1, 4)))
        lightMapMaterial = resourceFactory.createMaterial("lightMapMaterial", "./data/shaders/light.vert.spv", "./data/shaders/light.frag.spv", torchTexture, true, true, BlendMode.BLEND_FACTOR_SRC_COLOR, BlendMode.BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
        val lightTransform = Transform()
        lightTransform.z = 17.0f
        scene.addSimpleDraw(SimpleDraw(lightTransform, lightMap, lightMapMaterial))
    }

    fun switchCell(resourceFactory: ResourceFactory, cellX: Int, cellY: Int) {
        for (enemy in activeEnemies) {
            enemy.sprite.visible = false
            enemy.healthBar.sprite.visible = false
            enemy.collider.setActive(false)
        }

        for (container in activeContainers) {
            container.sprite.visible = false
            container.collider.setActive(false)
        }

        for (lights in activeLightSources) {
            val sprite = torchSystem.findSpriteComponent(lights.getId())
            if (sprite != null) {
                sprite.visible = false
            }

            val emitter = torchSystem.findEmitterComponent(lights.getId())!!
            emitter.enabled = false
        }

        activeEnemies.clear()
        activeContainers.clear()
        activeLightSources.clear()

        for (room in rooms) {
            val removeMe = ArrayList<Enemy>()
            for (enemy in room.enemies) {
                if (enemy.health <= 0) {
                    removeMe.add(enemy)
                    continue
                }

                if (enemy.cellX == cellX && enemy.cellY == cellY) {
                    enemy.sprite.visible = enemy.health > 0 && enemy.cellX == cellX && enemy.cellY == cellY
                    enemy.healthBar.sprite.visible = enemySystem.findSpriteComponent(enemy.getId())!!.visible
                    enemy.collider.setActive(enemy.sprite.visible)
                    if (enemy.sprite.visible) {
                        activeEnemies.add(enemy)
                    }
                }
            }

            for (enemy in removeMe) {
                room.enemies.remove(enemy)
            }

            for (container in room.containers) {
                if (container.cellX == cellX && container.cellY == cellY) {
                    container.sprite.visible = container.cellX == cellX && container.cellY == cellY
                    container.collider.setActive(container.sprite.visible)
                    val emitter = containerSystem.findBurstEmitterComponent(container.getId())!!
                    emitter.enabled = container.sprite.visible
                    activeContainers.add(container)
                }
            }

            for (light in room.torches) {
                if (light.cellX == cellX && light.cellY == cellY) {
                    val sprite = torchSystem.findSpriteComponent(light.getId())!!
                    sprite.visible = true

                    val emitter = torchSystem.findEmitterComponent(light.getId())!!
                    emitter.enabled = true
                    activeLightSources.add(light)
                }
            }

            for (light in room.campfire) {
                if (light.cellX == cellX && light.cellY == cellY) {
                    val emitter = torchSystem.findEmitterComponent(light.getId())!!
                    emitter.enabled = true
                    activeLightSources.add(light)
                }
            }
        }

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
                navMesh.map[i] = 127
                val e = Entity()
                collisionSystem.newEntity(e)
                        .attachTransformComponent()
                        .attachBoxColliderComponent(64.0f, 64.0f, BodyDef.BodyType.StaticBody)
                        .build()
                val tr = collisionSystem.findTransformComponent(e.getId())
                val cl = collisionSystem.findColliderComponent(e.getId())!!
                cl.setPosition(cx + 32.0f, cy + 32.0f)
                cl.setFriction(0.15f)
                tr!!.sx = 64.0f
                tr.sy = 64.0f
                tr.z = 13.0f
            }
            else {
                navMesh.map[i] = 0
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

        for (container in activeContainers) {
            if (container.cellX == cellX && container.cellY == cellY) {
                val ix: Int = (container.transform.x/64).toInt()
                val iy: Int = (container.transform.y/64).toInt()
                navMesh.map[ix + iy*width] = 127.toByte()
            }
        }

        generateLightMap()

        if (firstBuild) {
            backTilemap.create(resourceFactory, tilemapMaterial, width, height, 64.0f, 64.0f, backIndices)
            frontTilemap.create(resourceFactory, tilemapMaterial, width, height, 64.0f, 64.0f, frontIndices)
            detailTilemap.create(resourceFactory, tilemapMaterial, width, height, 64.0f, 64.0f, detailIndices)
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
            detailTilemap.update(detailIndices)
            backTilemap.transform.setPosition(0.0f, 0.0f, 1.0f)
            detailTilemap.transform.setPosition(0.0f, 0.0f, 1.01f)
            frontTilemap.transform.setPosition(0.0f, 0.0f, 10.0f)

            firstBuild = false
        }
        else {
            backTilemap.update(backIndices)
            frontTilemap.update(frontIndices)
            detailTilemap.update(detailIndices)
        }
    }

    private fun generateLightMap() {
        // Clear old light values
        for (i in 0 until lightValues.size) {
            lightValues[i] = Vector4f(0.0f, 0.0f, 0.0f, 0.1f)
        }

        // Put out light values
        for (light in activeLightSources) {
            val t = torchSystem.findTransformComponent(light.getId())!!
            val x = (t.x / 64.0f).toInt()
            val y = (t.y / 64.0f).toInt()
            lightValues[x + y * width] = Vector4f(light.color.x, light.color.y, light.color.z, 0.9f)
            spreadLight(x, y, lightValues[x + y * width])
        }

        // Put out light values at XpBalls
        for (xp in xpBallSystem.getEntityList()) {
            if (xp!!.sprite.visible) {
                val t = xpBallSystem.findTransformComponent(xp.getId())!!
                val x = (t.x / 64.0f).toInt()
                val y = (t.y / 64.0f).toInt()
                lightValues[x + y * width] = Vector4f(0.0f, 0.4f, 0.0f, 0.5f)
                spreadLight(x, y, lightValues[x + y * width])
            }
        }

        val px = ((player.transform.x) / 64.0f).toInt()
        val py = ((player.transform.y) / 64.0f).toInt()
        spreadLight(px, py, Vector4f(0.0f, 0.0f, 0.0f, 0.75f))

        var x = 0.0f
        var y = 0.0f
        var ix = 0
        var iy = 0
        var index = 0
        for (i in 0 until lightValues.size) {
            val top = if (iy > 0) { lightValues[ix + (iy-1)*width] } else { lightValues[i] }
            val bot = if (iy < height-1) { lightValues[ix + (iy+1)*width] } else { lightValues[i] }

            val topleft = if (ix > 0 && iy > 0) { lightValues[(ix-1) + (iy-1)*width] } else { lightValues[i] }
            val left = if (ix > 0) { lightValues[(ix-1) + iy*width] } else { lightValues[i] }
            val botleft = if (ix > 0 && iy < height-1) { lightValues[(ix-1) + (iy+1)*width]} else { lightValues[i] }

            val topRight = if (ix < width-1 && iy > 0) { lightValues[(ix+1) + (iy-1)*width]} else { lightValues[i] }
            val right = if (ix < width-1) { lightValues[(ix+1) + iy*width] } else { lightValues[i] }
            val botRight = if (ix < width-1 && iy < height-1) { lightValues[(ix+1) + (iy+1)*width] } else { lightValues[i] }

            lightVertices[index] = x
            lightVertices[index+1] = y
            lightVertices[index+2] = (lightValues[i].x+topleft.x+left.x+top.x)*0.25f
            lightVertices[index+3] = (lightValues[i].y+topleft.y+left.y+top.y)*0.25f
            lightVertices[index+4] = (lightValues[i].z+topleft.z+left.z+top.z)*0.25f
            lightVertices[index+5] = (lightValues[i].w+topleft.w+left.w+top.w)*0.25f

            lightVertices[index+6] = x
            lightVertices[index+7] = y+64.0f
            lightVertices[index+8] = (lightValues[i].x+botleft.x+left.x+bot.x)*0.25f
            lightVertices[index+9] = (lightValues[i].y+botleft.y+left.y+bot.y)*0.25f
            lightVertices[index+10] = (lightValues[i].z+botleft.z+left.z+bot.z)*0.25f
            lightVertices[index+11] = (lightValues[i].w+botleft.w+left.w+bot.w)*0.25f

            lightVertices[index+12] = x+64.0f
            lightVertices[index+13] = y+64.0f
            lightVertices[index+14] = (lightValues[i].x+botRight.x+right.x+bot.x)*0.25f
            lightVertices[index+15] = (lightValues[i].y+botRight.y+right.y+bot.y)*0.25f
            lightVertices[index+16] = (lightValues[i].z+botRight.z+right.z+bot.z)*0.25f
            lightVertices[index+17] = (lightValues[i].w+botRight.w+right.w+bot.w)*0.25f

            lightVertices[index+18] = x+64.0f
            lightVertices[index+19] = y+64.0f
            lightVertices[index+20] = (lightValues[i].x+botRight.x+right.x+bot.x)*0.25f
            lightVertices[index+21] = (lightValues[i].y+botRight.y+right.y+bot.y)*0.25f
            lightVertices[index+22] = (lightValues[i].z+botRight.z+right.z+bot.z)*0.25f
            lightVertices[index+23] = (lightValues[i].w+botRight.w+right.w+bot.w)*0.25f

            lightVertices[index+24] = x+64.0f
            lightVertices[index+25] = y
            lightVertices[index+26] = (lightValues[i].x+topRight.x+right.x+top.x)*0.25f
            lightVertices[index+27] = (lightValues[i].y+topRight.y+right.y+top.y)*0.25f
            lightVertices[index+28] = (lightValues[i].z+topRight.z+right.z+top.z)*0.25f
            lightVertices[index+29] = (lightValues[i].w+topRight.w+right.w+top.w)*0.25f

            lightVertices[index+30] = x
            lightVertices[index+31] = y
            lightVertices[index+32] = (lightValues[i].x+topleft.x+left.x+top.x)*0.25f
            lightVertices[index+33] = (lightValues[i].y+topleft.y+left.y+top.y)*0.25f
            lightVertices[index+34] = (lightValues[i].z+topleft.z+left.z+top.z)*0.25f
            lightVertices[index+35] = (lightValues[i].w+topleft.w+left.w+top.w)*0.25f

            index += 36
            x += 64.0f
            if (x >= width*64.0f) {
                x = 0.0f
                y += 64.0f
            }

            ix += 1
            if (ix >= width) {
                ix = 0
                iy += 1
            }
        }

        lightMap.update(lightVertices)
    }

    private fun spreadLight(x: Int, y: Int, value: Vector4f) {
        if (value.w <= 0.0f) {
            return
        }

        val att = 0.22f
        if (x in 0..(width - 1) && y >= 0 && y < height && lightValues[x + y * width].w < value.w) {
            val r = (lightValues[x + y * width].x + value.x) * 0.5f
            val g = (lightValues[x + y * width].y + value.y) * 0.5f
            val b = (lightValues[x + y * width].z + value.z) * 0.5f
            val a = (lightValues[x + y * width].w + value.w) * 0.5f
            lightValues[x + y * width] = Vector4f(r,g,b,a)
        }

        val mx = player.cellX * width + x
        val my = player.cellY * height + y
        val color = Vector4f(value.x, value.y, value.z, value.w - att)

        if (x in 1..width) {
            if (y in 0..(height - 1) && lightValues[(x-1) + y * width].w < color.w - att) {
                if (map[(mx-1) + my * mapWidth] == 0) {
                    spreadLight(x - 1, y, color)
                }
            }

            if (y in 1..height) {
                if (lightValues[(x-1) + (y-1) * width].w < color.w - att) {
                    if (map[(mx-1) + (my-1) * mapWidth] == 0) {
                        spreadLight(x - 1, y - 1, color)
                    }
                }
            }

            if (y in 0..(height-2)) {
                if (lightValues[(x-1) + (y+1) * width].w < color.w - att) {
                    if (map[(mx-1) + (my+1) * mapWidth] == 0) {
                        spreadLight(x - 1, y + 1, color)
                    }
                }
            }
        }

        if (x in 0..(width-2)) {
            if (y in 0..(height-1) && lightValues[(x+1) + y * width].w < color.w - att) {
                if (map[(mx+1) + my * mapWidth] == 0) {
                    spreadLight(x + 1, y, color)
                }
            }

            if (y in 1..height && lightValues[(x+1) + (y-1) * width].w < color.w - att) {
                if (map[(mx+1) + (my-1) * mapWidth] == 0) {
                    spreadLight(x + 1, y - 1, color)
                }
            }

            if (y in 0..(height-2) && lightValues[(x+1) + (y+1) * width].w < color.w - att) {
                if (map[(mx+1) + (my+1) * mapWidth] == 0) {
                    spreadLight(x + 1, y + 1, color)
                }
            }
        }

        if (x in 0..(width-1) && y in 1..height) {
            if (lightValues[x + (y-1) * width].w < color.w - att) {
                if (map[mx + (my-1) * mapWidth] == 0) {
                    spreadLight(x, y - 1, color)
                }
            }
        }

        if (x in 0..(width-1) && y in 0..(height-2)) {
            if (lightValues[x + (y+1) * width].w < color.w - att) {
                if (map[mx + (my+1) * mapWidth] == 0) {
                    spreadLight(x, y + 1, color)
                }
            }
        }
    }

    fun buildFirstRoom() {
        random = Random(System.currentTimeMillis())
        var x = 0
        var y = 0
        val firstRoomTiles = ArrayList<Vector2i>()
        for (i in 0 until width*height) {
            if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                map[x + (y*mapWidth)] = 1
            }
            else if (y == 1 && (x == 1 || x == width-2)) {
                map[x + (y*mapWidth)] = 1
            }
            else {
                firstRoomTiles.add(Vector2i(x, y))
            }

            x += 1
            if (x >= width) {
                x = 0
                y += 1
            }
        }

        val room = Room(firstRoomTiles, Vector4i(0,0,width,height),RoomType.DIRT_CAVE)
        rooms.add(room)

        mapBackIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapFrontIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapDetailIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        populateTilemap()

        // Set position of start and exit
        val startRoom = rooms[0]
        val endRoom = rooms[0]

        startPosition = Vector2i(width/2, 1)
        exitPosition = Vector2i(width/2, height/2)

        mapBackIndices[exitPosition.x + exitPosition.y * mapWidth] = TileIndex(2, endRoom.type.ordinal)
        generateLightsInRoom(room, width*2+height*2, false, torchSystem, torchMaterial)

        // Put campfire next to exit on first level
        val tx = exitPosition.x % width
        val ty = exitPosition.y % height
        val et = LightSource(exitPosition.x / width, exitPosition.y / height, Vector3f(0.9f, 0.55f, 0.1f))
        torchSystem.newEntity(et)
                .attachTransformComponent()
                .attachParticleEmitter(resourceFactory, 20, 40.0f, 0.7f, Vector2f(0.0f, -50.0f), DirectionType.LINEAR, 20.0f, 0.5f)
                .build()
        val etTransform = torchSystem.findTransformComponent(et.getId())
        etTransform!!.setPosition(((tx*64) + 32).toFloat(), ((ty*64) - 32).toFloat(), 18.0f)
        etTransform.sx = 64.0f
        etTransform.sy = 64.0f

        val emitter = torchSystem.findEmitterComponent(et.getId())!!
        emitter.startSize = 20.0f
        emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
        emitter.endColor.set(0.8f, 0.2f, 0.0f, 0.0f)
        emitter.enabled = false
        room.campfire.add(et)

        // Give the player a nice chest to start off with
        val container = Container(0, 5)
        containerSystem.newEntity(container)
                .attachTransformComponent()
                .attachSpriteComponent(itemMaterial)
                .attachBoxColliderComponent(64.0f, 48.0f, BodyDef.BodyType.StaticBody)
                .attachBurstParticleEmitter(resourceFactory, 25, 16.0f, 0.2f, Vector2f(0.0f, -50.0f), DirectionType.LINEAR, 32.0f, 0.5f)
                .build()

        val emitter2 = containerSystem.findBurstEmitterComponent (container.getId())!!
        emitter2.burstFinished = true
        emitter2.singleBurst = true
        emitter2.particlesPerBurst = 5
        emitter2.startColor = Vector4f(0.4f, 0.4f, 0.4f, 1.0f)
        emitter2.endColor = Vector4f(0.4f, 0.4f, 0.4f, 0.0f)
        emitter2.transform.z = 16.0f
        emitter2.enabled = true

        container.setPosition(Vector2i((tx+1)*64 + 32, ty*64 + 32))
        container.collider.setDensity(1000.0f)
        container.collider.setFriction(1.0f)
        container.sprite.visible = true
        container.collider.setActive(false)
        room.containers.add(container)
    }

    fun build(seed: Long, healthBarSystem: EntitySystem<HealthBar>, healthBarMaterial: Material) {
        random = Random(seed)
        generate(7)
        addWallBlockersAtEdges()
        buildRooms()

        mapBackIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapFrontIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        mapDetailIndices = Array(mapWidth*mapHeight){ TileIndexNone }
        populateTilemap()

        // Set position of start and exit
        val startRoom = rooms[0]
        val endRoom = rooms[0]

        startPosition = startRoom.findNoneEdgeTile(random)!!
        exitPosition = endRoom.findNoneEdgeTile(random)!!

        mapBackIndices[exitPosition.x + exitPosition.y * mapWidth] = TileIndex(2, endRoom.type.ordinal)

        generateRooms(healthBarSystem, healthBarMaterial)

        val pixelData = BufferUtils.createByteBuffer(mapWidth*mapHeight*3)
        for (room in rooms) {
            val R = random.nextInt(127).toByte()
            val G = random.nextInt(127).toByte()
            val B = random.nextInt(127).toByte()
            for (tile in room.tiles) {
                pixelData.put((tile.x + tile.y*mapWidth)*3, 127)
                pixelData.put((tile.x + tile.y*mapWidth)*3+1, 127)
                pixelData.put((tile.x + tile.y*mapWidth)*3+2, 127)
            }

            for (enemy in room.enemies) {
                val x = ((enemy.transform.x/64.0f)+(enemy.cellX*width)).toInt()
                val y = ((enemy.transform.y/64.0f)+(enemy.cellY*height)).toInt()

                pixelData.put((x + y*mapWidth)*3, 127)
                pixelData.put((x + y*mapWidth)*3+1, 0)
                pixelData.put((x + y*mapWidth)*3+2, 0)
            }

            for (container in room.containers) {
                val x = ((container.transform.x/64.0f)+(container.cellX*width)).toInt()
                val y = ((container.transform.y/64.0f)+(container.cellY*height)).toInt()
                pixelData.put((x + y*mapWidth)*3, 127)
                pixelData.put((x + y*mapWidth)*3+1, 100)
                pixelData.put((x + y*mapWidth)*3+2, 0)
            }
        }
        STBImageWrite.stbi_write_png("level.png", mapWidth, mapHeight, 3, pixelData, mapWidth*3)
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
                        mapFrontIndices[tile.x + (tile.y+1) * mapWidth] = TileIndex(0, 3)
                        map[tile.x + (tile.y + 1) * mapWidth] = 1
                    }
                }

                val r = random.nextInt(20)
                if (r == 1){
                    mapDetailIndices[tile.x + tile.y * mapWidth] = TileIndex(random.nextInt(3) + 4, tileY)
                }
            }

            for (campfire in room.campfire) {
                val transform = torchSystem.findTransformComponent(campfire.getId())!!
                val x = (transform.x / 64).toInt()
                val y = (transform.y / 64).toInt()
                mapDetailIndices[x + y * mapWidth] = TileIndex(0, 6)
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

        val maxTiles = 5
        if (x > 0) {
            if (mapCopy[(x-1) + y*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x-1, y, area, mapCopy, ArrayList()))
                if (tiles.size >= maxTiles) {
                    return tiles
                }
            }

            if (y > 0) {
                if (mapCopy[(x-1) + (y-1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x-1, y-1, area, mapCopy, ArrayList()))
                    if (tiles.size >= maxTiles) {
                        return tiles
                    }
                }
            }

            if (y < mapHeight - 1) {
                if (mapCopy[(x-1) + (y+1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x-1, y+1, area, mapCopy, ArrayList()))
                    if (tiles.size >= maxTiles) {
                        return tiles
                    }
                }
            }
        }

        if (y > 0) {
            if (mapCopy[x + (y-1)*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x, y-1, area, mapCopy, ArrayList()))
                if (tiles.size >= maxTiles) {
                    return tiles
                }
            }
        }

        if (x < mapWidth - 1) {
            if (mapCopy[(x+1) + y*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x+1, y, area, mapCopy, ArrayList()))
                if (tiles.size >= maxTiles) {
                    return tiles
                }
            }

            if (y > 0) {
                if (mapCopy[(x+1) + (y-1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x+1, y-1, area, mapCopy, ArrayList()))
                    if (tiles.size >= maxTiles) {
                        return tiles
                    }
                }
            }

            if (y < mapHeight - 1) {
                if (mapCopy[(x+1) + (y+1)*mapWidth] == 0) {
                    tiles.addAll(floodSearchRoom(x+1, y+1, area, mapCopy, ArrayList()))
                    if (tiles.size >= maxTiles) {
                        return tiles
                    }
                }
            }
        }

        if (y < mapHeight - 1) {
            if (mapCopy[x + (y+1)*mapWidth] == 0) {
                tiles.addAll(floodSearchRoom(x, y+1, area, mapCopy, ArrayList()))
                if (tiles.size >= maxTiles) {
                    return tiles
                }
            }
        }

        return tiles
    }

    private fun generateRooms(healthBarSystem: EntitySystem<HealthBar>, healthBarMaterial: Material) {
        for (room in rooms) {
            val lightCount = random.nextInt(5) + 3
            val thisRoomEnemyCount = (room.tiles.size/64)
            generateEnemiesInRoom(room, thisRoomEnemyCount, healthBarSystem, healthBarMaterial)
            val thisRoomContainerCount = room.tiles.size/72
            generateContainersInRoom(room, thisRoomContainerCount, containerSystem, itemMaterial)
            generateLightsInRoom(room, lightCount, random.nextInt(10) == 1, torchSystem, torchMaterial)
        }
    }

    private fun generateEnemiesInRoom(room: Room, count: Int, healthBarSystem: EntitySystem<HealthBar>, healthBarMaterial: Material) {
        for (i in 0 until count) {
            val p = room.findNoneEdgeTile(random)
            if (p == null) {
                return
            }

            val enemy = random.nextInt(2)

            val kracGuy = if (enemy == 0) {Krac(random)} else {MiniKrac(random)}
            enemySystem.newEntity(kracGuy)
                    .attachTransformComponent()
                    .attachSpriteComponent(enemyMaterial)
                    .attachAnimatorComponent()
                    .attachBoxColliderComponent(width = 60.0f, height = 40.0f, aliveOnStart = false)
                    .build()

            val levelFactor = (player.currentLevel*1.5f).toInt()
            kracGuy.strength = (random.nextInt(levelFactor) + levelFactor*5 * kracGuy.strengthFactor).toInt()
            kracGuy.agility = (random.nextInt(levelFactor) + levelFactor*2 * kracGuy.agilityFactor).toInt()
            kracGuy.health = (100 + random.nextInt(levelFactor) * kracGuy.healthFactor).toInt()
            kracGuy.sprite.visible = false

            val et = enemySystem.findTransformComponent(kracGuy.getId())!!
            kracGuy.collider.setDamping(0.0f)
            kracGuy.collider.setFriction(0.0f)
            kracGuy.healthBar.parentTransform = et

            healthBarSystem.newEntity(kracGuy.healthBar)
                    .attachTransformComponent()
                    .attachSpriteComponent(healthBarMaterial)
                    .build()

            kracGuy.healthBar.sprite.visible = false
            kracGuy.healthBar.transform.sx = 60.0f
            kracGuy.healthBar.transform.sy = 7.0f

            kracGuy.setPosition(Vector2i(p.x*64, p.y*64))
            room.enemies.add(kracGuy)
        }
    }

    private fun generateContainersInRoom(room: Room, count: Int, containerSystem: EntitySystem<Container>, containerMaterial: Material) {
        for (i in 0 until count) {
            val tile = room.findNoneEdgeTile(random)
            if (tile == null) {
                return
            }

            val container = Container(random.nextInt(2), random.nextInt(7) + 1)
            containerSystem.newEntity(container)
                    .attachTransformComponent()
                    .attachSpriteComponent(itemMaterial)
                    .attachBoxColliderComponent(64.0f, 48.0f, BodyDef.BodyType.StaticBody)
                    .attachBurstParticleEmitter(resourceFactory, 25, 16.0f, 0.2f, Vector2f(0.0f, -50.0f), DirectionType.LINEAR, 32.0f, 0.5f)
                    .build()

            val emitter = containerSystem.findBurstEmitterComponent (container.getId())!!
            emitter.burstFinished = true
            emitter.singleBurst = true
            emitter.particlesPerBurst = 5
            emitter.startColor = Vector4f(0.4f, 0.4f, 0.4f, 1.0f)
            emitter.endColor = Vector4f(0.4f, 0.4f, 0.4f, 0.0f)
            emitter.transform.z = 16.0f
            emitter.enabled = false

            container.setPosition(Vector2i(tile.x*64 + 32, tile.y*64 + 32))
            container.collider.setDensity(1000.0f)
            container.collider.setFriction(1.0f)
            container.sprite.visible = false
            container.collider.setActive(false)
            room.containers.add(container)
        }
    }

    private fun generateLightsInRoom(room: Room, torches: Int, campfire: Boolean, torchSystem: EntitySystem<LightSource>, torchMaterial: Material) {
        var numTorches = 0
        for (tile in room.tiles) {
            val x = tile.x
            val y = tile.y
            if (y > 0 && map[x + (y-1)*mapWidth] == 1) {
                val tx = x % width
                val ty = y % height
                val et = LightSource(x / width, y / height, Vector3f(0.9f, 0.55f, 0.1f))
                torchSystem.newEntity(et)
                        .attachTransformComponent()
                        .attachSpriteComponent(torchMaterial)
                        .attachParticleEmitter(resourceFactory, 10, 16.0f, 1.0f, Vector2f(0.0f, -10.0f), DirectionType.LINEAR, 4.0f, 0.5f)
                        .build()
                val etTransform = torchSystem.findTransformComponent(et.getId())
                etTransform!!.setPosition((tx*64 + 32).toFloat(), (ty*64 - 32).toFloat(), 18.0f)
                etTransform.sx = 48.0f
                etTransform.sy = 48.0f

                val sprite = torchSystem.findSpriteComponent(et.getId())!!
                sprite.visible = false

                val emitter = torchSystem.findEmitterComponent(et.getId())!!
                emitter.startSize = 5.0f
                emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
                emitter.endColor.set(1.0f, 0.3f, 0.0f, 0.5f)
                emitter.enabled = false
                room.torches.add(et)
                numTorches++
            }

            if (numTorches >= torches) {
                break
            }
        }

        if (campfire) {
            val tile = room.findNoneEdgeTile(random)
            if (tile != null) {
                val tx = tile.x % width
                val ty = tile.y % height
                val et = LightSource(tile.x / width, tile.y / height, Vector3f(0.9f, 0.55f, 0.1f))
                torchSystem.newEntity(et)
                        .attachTransformComponent()
                        .attachParticleEmitter(resourceFactory, 20, 40.0f, 0.7f, Vector2f(0.0f, -50.0f), DirectionType.LINEAR, 20.0f, 0.5f)
                        .build()
                val etTransform = torchSystem.findTransformComponent(et.getId())
                etTransform!!.setPosition(((tx*64) + 32).toFloat(), ((ty*64) - 32).toFloat(), 18.0f)
                etTransform.sx = 64.0f
                etTransform.sy = 64.0f

                val emitter = torchSystem.findEmitterComponent(et.getId())!!
                emitter.startSize = 20.0f
                emitter.startColor.set(1.0f, 0.9f, 0.2f, 1.0f)
                emitter.endColor.set(0.8f, 0.2f, 0.0f, 0.0f)
                emitter.enabled = false
                room.campfire.add(et)
            }
        }
    }
}
