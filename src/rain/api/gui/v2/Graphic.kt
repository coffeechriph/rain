package rain.api.gui.v2

import org.joml.Vector4f
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTruetype
import org.lwjgl.system.MemoryStack
import rain.api.gui.Font
import rain.api.gui.TextAlign
import rain.util.Earcut
import java.lang.Math.cos
import java.lang.Math.sin

internal fun gfxCreateText(tx: Float, ty: Float, w: Float, h: Float, textAlign: TextAlign, string: String, font: Font, color: Vector4f): FloatArray {
    val depth = 9.0f
    val scale = STBTruetype.stbtt_ScaleForPixelHeight(font.fontInfo, font.fontHeight)

    var textVertexDataIndex = 0
    val textVertexData = FloatArray(string.length*8*6)

    val paddingY = when(textAlign) {
        TextAlign.CENTER -> font.fontHeight
        TextAlign.LEFT -> font.fontHeight
        TextAlign.RIGHT -> font.fontHeight
        TextAlign.BOT_CENTER -> h
        TextAlign.BOT_LEFT -> h
        TextAlign.BOT_RIGHT -> h
        TextAlign.TOP_CENTER -> (font.descent+font.ascent)*scale
        TextAlign.TOP_LEFT -> (font.descent+font.ascent)*scale
        TextAlign.TOP_RIGHT -> (font.descent+font.ascent)*scale
    }

    val paddingX = when(textAlign) {
        TextAlign.CENTER -> (w / 2.0f - font.getStringWidth(string, 0, string.length) / 2.0f)
        TextAlign.LEFT -> 0.0f
        TextAlign.RIGHT -> w - font.getStringWidth(string, 0, string.length)
        TextAlign.BOT_CENTER -> w/2.0f - font.getStringWidth(string, 0, string.length)/2.0f
        TextAlign.BOT_LEFT -> 0.0f
        TextAlign.BOT_RIGHT -> w - font.getStringWidth(string, 0, string.length)
        TextAlign.TOP_CENTER -> w/2.0f - font.getStringWidth(string, 0, string.length)/2.0f
        TextAlign.TOP_LEFT -> 0.0f
        TextAlign.TOP_RIGHT -> w - font.getStringWidth(string, 0, string.length)
    }

    MemoryStack.stackPush().use { stack ->
        val codePoint = stack.mallocInt(1)
        val x = stack.floats(0.0f)
        val y = stack.floats(0.0f)
        val quad = STBTTAlignedQuad.mallocStack(stack)

        var index = 0
        val displayString = CharArray(string.length)
        var displayStringIndex = 0

        while(index < string.length) {
            displayString[displayStringIndex++] = string[index]
            index += font.getCodePoint(string, string.length, index, codePoint)
            val cp = codePoint.get(0)

            if (cp == '\n'.toInt()) {
                y.put(0, y.get(0) + (font.ascent - font.descent + font.lineGap) * scale)
                x.put(0, 0.0f)
            }
            else {
                font.getPackedQuad(cp, quad, x, y)
                if (font.useKerning && index + 1 < string.length) {
                    font.getCodePoint(string, string.length, index, codePoint)
                    x.put(0, x.get(0) + STBTruetype.stbtt_GetCodepointKernAdvance(font.fontInfo, cp, codePoint.get(0)) * scale)
                }

                val cx1 = tx + quad.x0() + paddingX
                val cx2 = tx + quad.x1() + paddingX
                val cy1 = ty + quad.y0() + paddingY
                val cy2 = ty + quad.y1() + paddingY
                val ux1 = quad.s0()
                val ux2 = quad.s1()
                val uy1 = quad.t0()
                val uy2 = quad.t1()

                textVertexData[textVertexDataIndex++] = cx1
                textVertexData[textVertexDataIndex++] = cy1
                textVertexData[textVertexDataIndex++] = depth
                textVertexData[textVertexDataIndex++] = color.x
                textVertexData[textVertexDataIndex++] = color.y
                textVertexData[textVertexDataIndex++] = color.z
                textVertexData[textVertexDataIndex++] = ux1
                textVertexData[textVertexDataIndex++] = uy1

                textVertexData[textVertexDataIndex++] = cx1
                textVertexData[textVertexDataIndex++] = cy2
                textVertexData[textVertexDataIndex++] = depth
                textVertexData[textVertexDataIndex++] = color.x
                textVertexData[textVertexDataIndex++] = color.y
                textVertexData[textVertexDataIndex++] = color.z
                textVertexData[textVertexDataIndex++] = ux1
                textVertexData[textVertexDataIndex++] = uy2

                textVertexData[textVertexDataIndex++] = cx2
                textVertexData[textVertexDataIndex++] = cy2
                textVertexData[textVertexDataIndex++] = depth
                textVertexData[textVertexDataIndex++] = color.x
                textVertexData[textVertexDataIndex++] = color.y
                textVertexData[textVertexDataIndex++] = color.z
                textVertexData[textVertexDataIndex++] = ux2
                textVertexData[textVertexDataIndex++] = uy2

                textVertexData[textVertexDataIndex++] = cx2
                textVertexData[textVertexDataIndex++] = cy2
                textVertexData[textVertexDataIndex++] = depth
                textVertexData[textVertexDataIndex++] = color.x
                textVertexData[textVertexDataIndex++] = color.y
                textVertexData[textVertexDataIndex++] = color.z
                textVertexData[textVertexDataIndex++] = ux2
                textVertexData[textVertexDataIndex++] = uy2

                textVertexData[textVertexDataIndex++] = cx2
                textVertexData[textVertexDataIndex++] = cy1
                textVertexData[textVertexDataIndex++] = depth
                textVertexData[textVertexDataIndex++] = color.x
                textVertexData[textVertexDataIndex++] = color.y
                textVertexData[textVertexDataIndex++] = color.z
                textVertexData[textVertexDataIndex++] = ux2
                textVertexData[textVertexDataIndex++] = uy1

                textVertexData[textVertexDataIndex++] = cx1
                textVertexData[textVertexDataIndex++] = cy1
                textVertexData[textVertexDataIndex++] = depth
                textVertexData[textVertexDataIndex++] = color.x
                textVertexData[textVertexDataIndex++] = color.y
                textVertexData[textVertexDataIndex++] = color.z
                textVertexData[textVertexDataIndex++] = ux1
                textVertexData[textVertexDataIndex++] = uy1
            }
        }
    }

    return textVertexData
}

internal fun gfxCreateRect(x: Float, y: Float, z: Float, width: Float, height: Float, color: Vector4f): FloatArray {
    val w = width
    val h = height
    return floatArrayOf(
        x, y, z, color.x, color.y, color.z,
        x, y+h, z, color.x, color.y, color.z,
        x+w, y+h, z, color.x, color.y, color.z,

        x+w, y+h, z, color.x, color.y, color.z,
        x+w, y, z, color.x, color.y, color.z,
        x, y, z, color.x, color.y, color.z
    )
}

// TODO: We don't actually have to triangulate such a simple mesh
// Change that in the future
internal fun gfxCreateRoundedRect(cx: Float, cy: Float, cz: Float, width: Float, height: Float, radius: Float, color: Vector4f): FloatArray {
    val w = width.toDouble()
    val h = height.toDouble()

    val list = ArrayList<Double>()
    list.addAll(listOf(0.0,  h - radius))
    list.addAll(listOf(0.0, 0.0 + radius))

    // First rounded corner - 16 vertices per rounded corner
    val angleStep = Math.PI * 0.5f / 8.0
    var px = 0.0
    var py = 0.0 + radius
    for (i in 0 until 8) {
        val x = px + (sin(-angleStep*i+Math.PI*1.5) * radius + radius).toFloat()
        val y = py + (cos(-angleStep*i+Math.PI*1.5) * radius).toFloat()
        list.addAll(listOf(x, y))
    }

    list.addAll(listOf(0.0 + radius, 0.0))
    list.addAll(listOf(w - radius, 0.0))

    px = w - radius
    py = 0.0
    for (i in 0 until 8) {
        val x = px + (sin(-angleStep*i+Math.PI) * radius).toFloat()
        val y = py + (cos(-angleStep*i+Math.PI) * radius + radius).toFloat()
        list.addAll(listOf(x, y))
    }

    list.addAll(listOf(w, 0.0 + radius))
    list.addAll(listOf(w, h - radius))
    px = w
    py = h - radius
    for (i in 0 until 8) {
        val x = px + (sin(-angleStep*i+Math.PI*0.5) * radius - radius).toFloat()
        val y = py + (cos(-angleStep*i+Math.PI*0.5) * radius).toFloat()
        list.addAll(listOf(x, y))
    }

    list.addAll(listOf(w - radius, h))
    list.addAll(listOf(0.0 + radius, h))
    px = 0.0 + radius
    py = h
    for (i in 0 until 8) {
        val x = px + (sin(-angleStep*i) * radius).toFloat()
        val y = py + (cos(-angleStep*i) * radius - radius).toFloat()
        list.addAll(listOf(x, y))
    }

    val indices = Earcut().triangulate(list.toDoubleArray(), null, 2)
    val vertices = ArrayList<Float>()
    for (i in indices) {
        vertices.add(cx + list[i*2].toFloat())
        vertices.add(cy + list[i*2+1].toFloat())
        vertices.add(cz)
        vertices.add(color.x)
        vertices.add(color.y)
        vertices.add(color.z)
        //vertices.add(color.w)
    }

    return vertices.toFloatArray()
}