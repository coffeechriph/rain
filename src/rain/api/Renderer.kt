package rain.api

/*
    Renderer interface implemented for different APIs and created once an
    API is chosen.
 */
interface Renderer {
    fun create()
    fun render()
    fun submitDrawSprite(transform: TransformComponent, vertexShader: Long, fragmentShader: Long)
}
