package example.roguelike.Entity

import rain.api.Input
import rain.api.entity.Entity
import rain.api.entity.EntitySystem
import rain.api.gui.Container
import rain.api.gui.Gui
import rain.api.gui.Text
import rain.api.gui.ToggleButton
import rain.api.scene.Scene

class Inventory(val gui: Gui, val player: Player) {
    var visible = true
    private var itemButtons = ArrayList<ToggleButton>()
    private var items = ArrayList<Item>()
    private lateinit var itemDescName: Text
    private lateinit var itemDescType: Text
    private lateinit var itemDescAgility: Text
    private lateinit var itemDescStamina: Text
    private lateinit var itemDescStrength: Text
    private lateinit var itemDescLuck: Text
    private val startX = 800
    private val startY = 0
    private var container: Container
    private var lastButtonClicked: ToggleButton? = null

    init {
        container = gui.newContainer(startX.toFloat(), startY.toFloat(), 1280.0f - startX, 720.0f)
    }

    fun addItem(item: Item) {
        val button = ToggleButton()
        button.text = item.name
        button.x = 250.0f
        button.y = (itemButtons.size*25).toFloat()
        button.w = 230.0f
        button.h = 25.0f
        items.add(item)
        itemButtons.add(button)
        container.addComponent(button)
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
                    container.removeText(itemDescStamina)
                    container.removeText(itemDescStrength)
                    container.removeText(itemDescAgility)
                    container.removeText(itemDescLuck)
                }

                itemDescName = container.addText("Name: ${item.name}", 0.0f, 20.0f, background = true)
                itemDescStamina = container.addText("Stamina: ${item.stamina}", 0.0f, 40.0f, background = true)
                itemDescStrength = container.addText("Strength: ${item.strength}", 0.0f, 60.0f, background = true)
                itemDescAgility = container.addText("Agility: ${item.agility}", 0.0f, 80.0f, background = true)
                itemDescLuck = container.addText("Luck: ${item.luck}", 0.0f, 100.0f, background = true)
            }
        }
    }
}
