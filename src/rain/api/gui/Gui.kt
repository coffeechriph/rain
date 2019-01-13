package rain.api.gui

import rain.api.Input
import rain.api.gfx.Material
import rain.api.gfx.Renderer
import rain.api.gfx.ResourceFactory

class Gui(private val resourceFactory: ResourceFactory, private val renderer: Renderer) {
    private val containers = ArrayList<Container>()
    private lateinit var componentMaterial: Material
    private lateinit var textMaterial: Material
    lateinit var font: Font

    fun init() {
        font = Font("./data/fonts/FreeSans.ttf")
        font.buildBitmap(resourceFactory, 1024, 1024, 20.0f)
        textMaterial = resourceFactory.buildMaterial()
                .withName("guiTextMaterial")
                .withVertexShader("./data/shaders/text.vert.spv")
                .withFragmentShader("./data/shaders/text.frag.spv")
                .withTexture(font.texture)
                .build()

        componentMaterial = resourceFactory.buildMaterial()
                .withName("guiMaterial")
                .withVertexShader("./data/shaders/gui.vert.spv")
                .withFragmentShader("./data/shaders/gui.frag.spv")
                .build()
    }

    fun newContainer(x: Float, y: Float, w: Float, h: Float): Container {
        val c = Container(componentMaterial, textMaterial, resourceFactory, font)
        c.transform.x = x
        c.transform.y = y
        c.transform.sx = w
        c.transform.sy = h
        containers.add(c)
        return c
    }

    fun update(input: Input) {
        for (container in containers) {
            container.update(input)
        }

        for (container in containers) {
            if (!container.isDirty) {
                continue
            }

            container.build(renderer)
        }
    }

    fun render() {
        for (container in containers) {
            container.render(renderer)
        }
    }

    fun clear() {
        containers.clear()

        if (::font.isInitialized) {
            font.destroy()
        }
    }
}
