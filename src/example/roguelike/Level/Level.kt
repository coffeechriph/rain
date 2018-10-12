package example.roguelike.Level

import org.joml.Vector3f
import rain.api.*
import java.util.*

class Level {
    private lateinit var map: IntArray
    private var width = 0
    private var height = 0
    private lateinit var material: Material
    private lateinit var texture: Texture2d
    var tilemap = Tilemap()
        private set
    private var firstBuild = true

    fun create(resourceFactory: ResourceFactory, width: Int, height: Int) {
        texture = resourceFactory.createTexture2d("./data/textures/tiles.png", TextureFilter.NEAREST)
        texture.setTiledTexture(16,16)
        material = resourceFactory.createMaterial("./data/shaders/tilemap.vert.spv", "./data/shaders/basic.frag.spv", texture, Vector3f(1.0f,1.0f, 1.0f))

        this.width = width
        this.height = height
        map = IntArray(width*height)
    }

    fun build(resourceFactory: ResourceFactory, seed: Long) {
        generate(seed, 3)

        val tileIndices = Array(width*height){ Tilemap.TileIndex(0,0)}
        for (i in 0 until width * height) {
            if (map[i] == 0) {
                tileIndices[i] = Tilemap.TileIndex(2, 0)
            }
        }

        if (firstBuild) {
            tilemap.create(resourceFactory, material, width, height, 32.0f, 32.0f, tileIndices)
            firstBuild = false
        }
        else {
            tilemap.update(tileIndices)
        }
    }

    private fun generate(seed: Long, repeats: Int) {
        var x = 0
        var y = 0
        var random = Random(seed)
        for (i in 0 until map.size) {
            map[i] = random.nextInt(2)

            x += 1
            if (x >= width) {
                x = 0
                y += 1
            }
        }

        repeat(repeats) {
            var x1 = 0
            var y1 = 0
            for (i in 0 until map.size) {
                val c = numNeighbours(x1,y1)

                if (map[x1 + y1 * width] == 1 && c < 4) {
                    map[x1 + y1 * width] = 0
                }
                else if (map[x1 + y1 * width] == 0 && c >= 5) {
                    map[x1 + y1 * width] = 1
                }

                x1 += 1
                if (x1 >= width) {
                    x1 = 0
                    y1 += 1
                }
            }
        }
    }

    private fun numNeighbours(x: Int, y: Int): Int {
        var count = 0
        if (x > 0) {
            if (map[(x-1) + y * width] == 1) {
                count += 1
            }

            if (y > 0) {
                if (map[(x-1) + (y-1) * width] == 1) {
                    count += 1
                }
            }

            if (y < height - 1) {
                if (map[(x-1) + (y+1) * width] == 1) {
                    count += 1
                }
            }
        }

        if (x < width - 1) {
            if (map[(x+1) + y * width] == 1) {
                count += 1
            }

            if (y > 0) {
                if (map[(x+1) + (y-1) * width] == 1) {
                    count += 1
                }
            }

            if (y < height - 1) {
                if (map[(x+1) + (y+1) * width] == 1) {
                    count += 1
                }
            }
        }

        if (y > 0) {
            if (map[x + (y-1) * width] == 1) {
                count += 1
            }
        }

        if (y < height - 1) {
            if (map[x + (y+1) * width] == 1) {
                count += 1
            }
        }

        return count
    }
}