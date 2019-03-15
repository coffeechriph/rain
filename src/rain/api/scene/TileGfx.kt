package rain.api.scene

val TileGfxNone = TileGfx(-1, -1, 1.0f, 1.0f, 1.0f, 0.0f)
data class TileGfx(val x: Int, val y: Int, var red: Float = 1.0f, var green: Float = 1.0f, var
blue: Float = 1.0f, var alpha: Float = 1.0f)
