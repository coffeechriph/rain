package rain.api

import org.joml.Vector3f

/*
    Generic Material handler for specifying properties to match against.
    The specific backend implementation (Vulkan/OpenGL) will choose best suited
    pipeline to render any Entity that is using this handler

    vertexShader and fragmentShader is simply an ID for the backend to keep track of
 */
class Material internal constructor(val vertexShader: Long, val fragmentShader: Long, val texture: Texture2d, val color: Vector3f)
