package example.roguelike.Entity

import rain.api.gui.Container
import rain.api.gui.Gui
import rain.api.gui.Text
import rain.api.gui.ToggleButton

class Inventory(val gui: Gui, val player: Player) {
    var visible = true
        set(value) {
            field = value
            container.visible = value
        }
    private var itemButtons = ArrayList<ToggleButton>()
    private var items = ArrayList<Item>()
    private lateinit var headerText: Text
    private lateinit var itemDescName: Text
    private lateinit var itemDescType: Text
    private lateinit var itemDescAgility: Text
    private lateinit var itemDescStamina: Text
    private lateinit var itemDescStrength: Text
    private lateinit var itemDescLuck: Text
    private val startX = 1280 / 2.0f - 250.0f
    private val startY = 0.0f
    private var container: Container
    private var lastButtonClicked: ToggleButton? = null

    init {
        container = gui.newContainer(startX, startY, 500.0f, 720.0f)
    }

    fun addItem(item: Item) {
        val button = ToggleButton()
        button.text = item.name
        button.x = 0.0f
        button.y = 20.0f + (itemButtons.size*25).toFloat()
        button.w = 230.0f
        button.h = 25.0f
        items.add(item)
        itemButtons.add(button)
        container.addComponent(button)

        if (::headerText.isInitialized) {
            container.removeText(headerText)
        }

        headerText = container.addText("<Inventory: ${items.size}>", 0.0f, 0.0f, background = true)
        headerText.x = 115.0f - headerText.w/2.0f
    }

    fun update() {
        if (itemButtons.size > 0) {
            var index = -1
            for (i in 0 until itemButtons.size) {
                if (itemButtons[i].active && itemButtons[i] != lastButtonClicked) {
                    index = i
                    break
                }
            }

            for (i in 0 until itemButtons.size) {
                itemButtons[i].active = false
            }

            if (index > -1) {
                lastButtonClicked = itemButtons[index]
                itemButtons[index].active = true
                val item = items[index]
                if (::itemDescName.isInitialized) {
                    container.removeText(itemDescName)
                    container.removeText(itemDescType)
                    container.removeText(itemDescStamina)
                    container.removeText(itemDescStrength)
                    container.removeText(itemDescAgility)
                    container.removeText(itemDescLuck)
                }

                itemDescName = container.addText("Name: ${item.name}", 230.0f, 20.0f, background = true)
                itemDescType = container.addText("Type: ${item.type.name}", 230.0f, 40.0f, background = true)
                itemDescStamina = container.addText("Stamina: ${item.stamina}", 230.0f, 60.0f, background = true)
                itemDescStrength = container.addText("Strength: ${item.strength}", 230.0f, 80.0f, background = true)
                itemDescAgility = container.addText("Agility: ${item.agility}", 230.0f, 100.0f, background = true)
                itemDescLuck = container.addText("Luck: ${item.luck}", 230.0f, 120.0f, background = true)
            }

            if (lastButtonClicked != null) {
                lastButtonClicked!!.active = true
            }
        }
    }
}
