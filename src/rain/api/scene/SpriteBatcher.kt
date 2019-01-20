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
import rain.log
import rain.vulkan.VertexAttribute
import java.nio.ByteBuffer

internal class SpriteBatcher(private val system: EntitySystem<Entity>, val material: Material, private val resourceFactory: ResourceFactory) {
    lateinit var vertexBuffer: VertexBuffer
    private val sprites = ArrayList<Sprite>()

    private lateinit var vertexData: FloatArray
    private lateinit var instanceData: ByteBuffer

    internal fun hasSprites(): Boolean{
        return sprites.isNotEmpty()
    }

    internal fun batch() {
        if (system.getSpriteList().isEmpty()) {
            return
        }

        val numSpritesLastFrame = this.sprites.size
        this.sprites.clear()
        for (sprite in system.getSpriteList()) {
            if (sprite!!.visible) {
                this.sprites.add(sprite)
            }
        }

        if (this.sprites.size != numSpritesLastFrame) {
            log("Batching ${this.sprites.size} sprites")
            vertexData = FloatArray(system.getSpriteList().size * 6 * 5)

            var index = 0
            var vIndex = 0
            for (sprite in this.sprites) {
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
                // TODO: Optimize this!
                val byteBuffer = memAlloc(vertexData.size*4)
                byteBuffer.asFloatBuffer().put(vertexData).flip()
                vertexBuffer = resourceFactory.buildVertexBuffer()
                        .withVertices(byteBuffer)
                        .withState(VertexBufferState.DYNAMIC)
                        .withAttribute(VertexAttribute(0, 2))
                        .withAttribute(VertexAttribute(1, 2))
                        .withAttribute(VertexAttribute(2, 1))
                        .build()
            }
            else {
                // TODO: Optimize this!
                val byteBuffer = memAlloc(vertexData.size*4)
                byteBuffer.asFloatBuffer().put(vertexData).flip()
                vertexBuffer.update(byteBuffer)
            }
        }
    }

    internal fun update() {
        if (::instanceData.isInitialized) {
            if (instanceData.remaining() <= sprites.size * 32 * 4) {
                memFree(instanceData)
                instanceData = memAlloc(sprites.size * 32 * 4)
            }
        }
        else {
            instanceData = memAlloc(sprites.size * 32 * 4)
        }

        for (sprite in sprites) {
            instanceData.put(sprite.getUniformData())
        }
        instanceData.flip()
        material.getTexelBuffer().update(instanceData)
    }
}