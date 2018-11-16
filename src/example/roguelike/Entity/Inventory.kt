package example.roguelike.Entity

import rain.api.gui.*
import rain.api.gui.Container

class Inventory(val gui: Gui, val player: Player) {
    var visible = true
        set(value) {
            field = value
            container.visible = value
        }

    private val ItemNone = Item(ItemType.NONE, "Empty", 0, 0, 0, 0)
    var equippedWeapon: Item = ItemNone
    var equippedHead: Item = ItemNone
    var equippedGloves: Item = ItemNone
    var equippedBoots: Item = ItemNone
    var equippedChest: Item = ItemNone
    var equippedLegs: Item = ItemNone
    private lateinit var equippedWeaponText: Text
    private lateinit var equippedHeadText: Text
    private lateinit var equippedGlovesText: Text
    private lateinit var equippedBootsText: Text
    private lateinit var equippedChestText: Text
    private lateinit var equippedLegsText: Text

    private var itemButtons = ArrayList<ToggleButton>()
    private var equipButton = Button()
    private var items = ArrayList<Item>()
    private lateinit var headerText: Text
    private lateinit var itemDescName: Text
    private lateinit var itemDescType: Text
    private lateinit var itemDescAgility: Text
    private lateinit var itemDescStamina: Text
    private lateinit var itemDescStrength: Text
    private lateinit var itemDescLuck: Text
    private val startX = 1280 / 2.0f - 115.0f
    private val startY = 0.0f
    private var container: Container
    private var lastButtonClicked: ToggleButton? = null
    private var selectedItem: Item = ItemNone

    init {
        container = gui.newContainer(startX, startY, 500.0f, 720.0f)
        container.visible = false
        equipButton.x = 230.0f
        equipButton.y = 140.0f
        equipButton.w = 100.0f
        equipButton.h = 20.0f
        equipButton.text = "Equip"
        container.addComponent(equipButton)

        equippedWeaponText = container.addText("Weapon: ${equippedWeapon.name}", 230.0f, 160.0f, background = true)
        equippedHeadText = container.addText("Head: ${equippedHead.name}", 230.0f, 180.0f, background = true)
        equippedChestText = container.addText("Chest: ${equippedChest.name}", 230.0f, 200.0f, background = true)
        equippedGlovesText = container.addText("Gloves: ${equippedGloves.name}", 230.0f, 220.0f, background = true)
        equippedBootsText = container.addText("Boots: ${equippedBoots.name}", 230.0f, 240.0f, background = true)
        equippedLegsText = container.addText("Legs: ${equippedLegs.name}", 230.0f, 260.0f, background = true)
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
                selectedItem = items[index]
                if (::itemDescName.isInitialized) {
                    container.removeText(itemDescName)
                    container.removeText(itemDescType)
                    container.removeText(itemDescStamina)
                    container.removeText(itemDescStrength)
                    container.removeText(itemDescAgility)
                    container.removeText(itemDescLuck)
                }

                itemDescName = container.addText("Name: ${selectedItem.name}", 230.0f, 20.0f, background = true)
                itemDescType = container.addText("Type: ${selectedItem.type.name}", 230.0f, 40.0f, background = true)
                itemDescStamina = container.addText("Stamina: ${selectedItem.stamina}", 230.0f, 60.0f, background = true)
                itemDescStrength = container.addText("Strength: ${selectedItem.strength}", 230.0f, 80.0f, background = true)
                itemDescAgility = container.addText("Agility: ${selectedItem.agility}", 230.0f, 100.0f, background = true)
                itemDescLuck = container.addText("Luck: ${selectedItem.luck}", 230.0f, 120.0f, background = true)
            }

            if (lastButtonClicked != null) {
                lastButtonClicked!!.active = true
            }
        }

        if (equipButton.active) {
            when (selectedItem.type) {
                ItemType.CHEST -> equippedChest = selectedItem
                ItemType.GLOVES -> equippedGloves = selectedItem
                ItemType.MELEE -> equippedWeapon = selectedItem
                ItemType.RANGED -> equippedWeapon = selectedItem
                ItemType.LEGS -> equippedLegs = selectedItem
                ItemType.BOOTS -> equippedBoots = selectedItem
                ItemType.HEAD -> equippedHead = selectedItem
            }

            container.removeText(equippedWeaponText)
            container.removeText(equippedHeadText)
            container.removeText(equippedChestText)
            container.removeText(equippedGlovesText)
            container.removeText(equippedBootsText)
            container.removeText(equippedLegsText)

            equippedWeaponText = container.addText("Weapon: ${equippedWeapon.name}", 230.0f, 170.0f, background = true)
            equippedHeadText = container.addText("Head: ${equippedHead.name}", 230.0f, 190.0f, background = true)
            equippedChestText = container.addText("Chest: ${equippedChest.name}", 230.0f, 210.0f, background = true)
            equippedGlovesText = container.addText("Gloves: ${equippedGloves.name}", 230.0f, 230.0f, background = true)
            equippedBootsText = container.addText("Boots: ${equippedBoots.name}", 230.0f, 250.0f, background = true)
            equippedLegsText = container.addText("Legs: ${equippedLegs.name}", 230.0f, 270.0f, background = true)
        }
    }
}
