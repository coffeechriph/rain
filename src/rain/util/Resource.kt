package rain.util

import org.lwjgl.BufferUtils
import rain.log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


internal @Throws(IOException::class)
fun ioResourceToByteBuffer(resource: String, bufferSize: Int): ByteBuffer {
    var buffer: ByteBuffer
    val url = Thread.currentThread().contextClassLoader.getResource(resource)
    val file = File(url!!.file)
    if (file.isFile) {
        val fis = FileInputStream(file)
        val fc = fis.channel
        buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
        fc.close()
        fis.close()
    } else {
        buffer = BufferUtils.createByteBuffer(bufferSize)
        val source = url.openStream() ?: throw FileNotFoundException(resource)
        try {
            val buf = ByteArray(8192)
            while (true) {
                val bytes = source.read(buf, 0, buf.size)
                if (bytes == -1)
                    break
                if (buffer.remaining() < bytes)
                    buffer = resizeBuffer(buffer, buffer.capacity() * 2)
                buffer.put(buf, 0, bytes)
            }
            buffer.flip()
        } finally {
            source.close()
        }
    }
    return buffer
}

fun readFileAsByteBuffer(filePath: String): ByteBuffer {
    val file = File(filePath)
    if (file.exists()) {
        val fileStream = FileInputStream(file)
        val data = fileStream.readAllBytes()
        val buf = BufferUtils.createByteBuffer(data.size)

        buf.put(data)
        buf.flip()
        fileStream.close()
        log("Loaded $filePath with size ${data.size} bytes.")
        return buf
    }

    throw FileNotFoundException("Could not find file $filePath")
}

private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
    val newBuffer = BufferUtils.createByteBuffer(newCapacity)
    buffer.flip()
    newBuffer.put(buffer)
    return newBuffer
}
