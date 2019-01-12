package rain.api.scene

import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.entity.Sprite
import rain.api.gfx.Material
import rain.api.gfx.ResourceFactory
import rain.api.gfx.VertexBuffer
import rain.api.gfx.VertexBufferState
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

internal class SpriteBatcher(private val system: EntitySystem<Entity>, val material: Material, private val resourceFactory: ResourceFactory) {
    lateinit var vertexBuffer: VertexBuffer
    private val sprites = ArrayList<Sprite>()

    private lateinit var vertexData: FloatArray
    private lateinit var instanceData: ByteBuffer

    internal fun batch() {
        if (this.sprites.size != system.getSpriteList().size) {
            this.sprites.clear()

            if (::vertexData.isInitialized) {
                memFree(instanceData)
            }

            vertexData = FloatArray(system.getSpriteList().size * 6 * 5)
            instanceData = memAlloc(system.getSpriteList().size * 16 * 4)

            var index = 0
            var vIndex = 0
            for (sprite in system.getSpriteList()) {
                this.sprites.add(sprite!!)

                vertexData[vIndex++] = -0.5f
                vertexData[vIndex++] = -0.5f
                vertexData[vIndex++] = 0.0f
                vertexData[vIndex++] = 0.0f
                vertexData[vIndex++] = index.toFloat()

                vertexData[vIndex++] = -0.5f
                vertexData[vIndex++] = 0.5f
                vertexData[vIndex++] = 0.0f
                vertexData[vIndex++] = 1.0f
                vertexData[vIndex++] = index.toFloat()

                vertexData[vIndex++] =  0.5f
                vertexData[vIndex++] = 0.5f
                vertexData[vIndex++] = 1.0f
                vertexData[vIndex++] = 1.0f
                vertexData[vIndex++] = index.toFloat()

                vertexData[vIndex++] =  0.5f
                vertexData[vIndex++] = 0.5f
                vertexData[vIndex++] = 1.0f
                vertexData[vIndex++] = 1.0f
                vertexData[vIndex++] = index.toFloat()

                vertexData[vIndex++] =  0.5f
                vertexData[vIndex++] = -0.5f
                vertexData[vIndex++] = 1.0f
                vertexData[vIndex++] = 0.0f
                vertexData[vIndex++] = index.toFloat()

                vertexData[vIndex++] = -0.5f
                vertexData[vIndex++] = -0.5f
                vertexData[vIndex++] = 0.0f
                vertexData[vIndex++] = 0.0f
                vertexData[vIndex++] = index.toFloat()
                index += 1
            }

            if (!::vertexBuffer.isInitialized) {
                vertexBuffer = resourceFactory.createVertexBuffer(vertexData, VertexBufferState.DYNAMIC, arrayOf(VertexAttribute(0, 2), VertexAttribute(1, 2), VertexAttribute(2, 1)))
                //texelBuffer = resourceFactory.createTexelBuffer()
            }
            else {
                vertexBuffer.update(vertexData)
            }
        }
    }

    internal fun update() {
        if (::instanceData.isInitialized) {
            if (instanceData.capacity() <= sprites.size * 22 * 4) {
                memFree(instanceData)
                instanceData = memAlloc(sprites.size * 22 * 4)
            }
        }
        else {
            instanceData = memAlloc(sprites.size * 22 * 4)
        }

        for (sprite in sprites) {
            instanceData.put(sprite.getUniformData())
        }
        instanceData.flip()
        material.getTexelBuffer().update(instanceData)
    }
}