package rain.api.gui

import org.joml.Vector3f
import rain.api.Input
import rain.api.gfx.*

class Gui(private val resourceFactory: ResourceFactory, private val renderer: Renderer) {
    private val containers = ArrayList<Container>()
    private val material: Material

    var font: Font
    init {
        val guiSkin = resourceFactory.loadTexture2d("./data/textures/skin.png", TextureFilter.NEAREST)
        font = Font("./data/fonts/FreeSans.ttf")
        font.buildBitmap(resourceFactory, 1024, 1024, 20.0f)
        material = resourceFactory.createMaterial("./data/shaders/gui.vert.spv", "./data/shaders/gui.frag.spv", arrayOf(guiSkin, font.texture), Vector3f(1.0f, 1.0f, 1.0f))
    }

    fun newContainer(x: Float, y: Float, w: Float, h: Float): Container {
        val c = Container(material, resourceFactory, font)
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

            container.build()
        }
    }

    fun render() {
        for (container in containers) {
            container.render(renderer)
        }
    }
}
