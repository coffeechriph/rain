package rain.api.gui

import org.lwjgl.stb.*
import rain.util.readFileAsByteBuffer
import java.nio.ByteBuffer
import java.nio.IntBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.stb.STBImageWrite.stbi_write_png
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryUtil.memAlloc
import rain.api.gfx.ResourceFactory
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import java.nio.FloatBuffer
import org.lwjgl.stb.STBTruetype.stbtt_PackEnd
import javax.swing.Spring.scale
import org.lwjgl.stb.STBTruetype.stbtt_PackFontRange
import org.lwjgl.stb.STBTruetype.stbtt_PackSetOversampling

class Font(ttfFile: String) {
    var useKerning = true
    var fontHeight: Float
        private set
    val fontInfo: STBTTFontinfo
    lateinit var texture: Texture2d
        private set

    var ascent: Int
    var descent: Int
    var lineGap: Int

    private val ttf: ByteBuffer
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private lateinit var cdata: STBTTPackedchar.Buffer

    init {
        ttf = readFileAsByteBuffer(ttfFile)
        fontInfo = STBTTFontinfo.create()
        if (!stbtt_InitFont(fontInfo, ttf)) {
            throw IllegalStateException("Failed to initialize font!")
        }
        var ascent = 0
        var descent = 0
        var lineGap = 0
        stackPush().use { stack ->
            val pAscent = stack.mallocInt(1)
            val pDescent = stack.mallocInt(1)
            val pLineGap = stack.mallocInt(1)

            stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)

            ascent = pAscent.get(0)
            descent = pDescent.get(0)
            lineGap = pLineGap.get(0)
        }
        this.ascent = ascent
        this.descent = descent
        this.lineGap = lineGap
        this.fontHeight = 24.0f
    }

    fun buildBitmap(resourceFactory: ResourceFactory, width: Int, height: Int, pixelHeight: Float) {
        cdata = STBTTPackedchar.malloc(2 * 512)

        val startChar = 32
        val numChars = 512
        val limit = numChars - startChar

        val bitmap = memAlloc(width*height)
        val pc = STBTTPackContext.malloc()
        stbtt_PackBegin(pc, bitmap, width, height, 0, 1, 0)

        var p = numChars + startChar
        cdata.limit(p + limit)
        cdata.position(p)
        stbtt_PackSetOversampling(pc, 1, 1)
        stbtt_PackFontRange(pc, ttf, 0, pixelHeight, startChar, cdata)

        p = 1 * numChars + startChar
        cdata.limit(p + limit)
        cdata.position(p)
        stbtt_PackSetOversampling(pc, 2, 2)
        stbtt_PackFontRange(pc, ttf, 0, pixelHeight, startChar, cdata)

        stbtt_PackEnd(pc)

        texture = resourceFactory.createTexture2d(bitmap, width, height, 1, TextureFilter.NEAREST)
        fontHeight = pixelHeight
        bitmapWidth = width
        bitmapHeight = height
    }

    fun getPackedQuad(c: Int, quad: STBTTAlignedQuad, x: FloatBuffer, y: FloatBuffer) {
        stbtt_GetPackedQuad(cdata, bitmapWidth, bitmapHeight, c - 32, x, y, quad, true)
    }

    fun getStringWidth(text: String, from: Int, to: Int): Float {
        var width = 0
        stackPush().use { stack ->
            val pCodePoint = stack.mallocInt(1)
            val pAdvancedWidth = stack.mallocInt(1)
            val pLeftSideBearing = stack.mallocInt(1)

            var i = from
            while (i < to) {
                i += getCodePoint(text, to, i, pCodePoint)
                val cp = pCodePoint.get(0)

                stbtt_GetCodepointHMetrics(fontInfo, cp, pAdvancedWidth, pLeftSideBearing)
                width += pAdvancedWidth.get(0)

                if (useKerning && i < to) {
                    getCodePoint(text, to, i, pCodePoint)
                    width += stbtt_GetCodepointKernAdvance(fontInfo, cp, pCodePoint.get(0))
                }
            }
        }

        return width * stbtt_ScaleForPixelHeight(fontInfo, fontHeight)
    }

    fun getCodePoint(text: String, to: Int, i: Int, cpOut: IntBuffer): Int {
        val c1 = text[i]
        if (Character.isHighSurrogate(c1) && i + 1 < to) {
            val c2 = text[i + 1]
            if (Character.isLowSurrogate(c2)) {
                cpOut.put(0, Character.toCodePoint(c1, c2))
                return 2
            }
        }
        cpOut.put(0, c1.toInt())
        return 1
    }

    fun destroy() {
        fontInfo.clear()
    }
}
