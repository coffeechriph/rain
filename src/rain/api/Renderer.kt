package rain.api

import org.joml.Vector2i

/*
    Renderer interface implemented for different APIs and created once an
    API is chosen.
 */
interface Renderer {
    fun create()
    fun render()
    fun submitDrawSprite(transform: TransformComponent, material: Material, textureTileOffset: Vector2i)
    fun submitDrawTilemap(tilemap: Tilemap)
}
