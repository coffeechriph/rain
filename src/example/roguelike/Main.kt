package example.roguelike

import org.joml.Vector2i
import rain.api.Api
import rain.api.scene.NavMesh
import java.util.*

fun main(args: Array<String>) {
    val map = byteArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 2, 2, 0, 0, 0,
            0, 127, 127, 2, 0, 127, 127, 127,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0
    )
    val mesh = NavMesh(8,8)
    mesh.fillMap(map)

    val rand = Random()
    for (i in 0 until 3) {
        val end = Vector2i(rand.nextInt(8), rand.nextInt(8))
        val path = mesh.findPath(Vector2i(0, 0), Vector2i(end.x, end.y))
        val cp = map.clone()
        for (p in path) {
            cp[p.x + p.y * 8] = 1
        }

        cp[end.x + end.y * 8] = 66

        println("##################")
        for (i in 0 until cp.size) {
            if (i != 0 && i % 8 == 0) {
                print("\n")
            }

            if (cp[i] == 127.toByte()) {
                print("# ")
            }
            else if (cp[i] == 66.toByte()) {
                print("G ")
            }
            else {
                print("${cp[i]} ")
            }
        }
        println()
    }

    val app = Roguelike()
    app.create(1280,720,"Hello Rain!", Api.VULKAN)
    app.run()
}
