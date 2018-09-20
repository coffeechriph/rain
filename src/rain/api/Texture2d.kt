package rain.api

/*
    A 2d texture handler for specifying properties to match against.
    The specific backend implementation (Vulkan/OpenGL) will choose best suited
    pipeline to render any Entity that is using a Material which includes this handler

    The handler is simply an ID for the backend to keep track of
    TODO: Support more formats than 2d textures
 */
class Texture2d internal constructor(val handler: Int, val width: Int, val height: Int, val filter: TextureFilter)
