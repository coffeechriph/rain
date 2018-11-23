package example.roguelike.Entity

import rain.api.gui.*
import rain.api.gui.Container

class Inventory(val gui: Gui, val player: Player) {
    var visible = false
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
    private var equippedWeaponText: Text
    private var equippedHeadText: Text
    private var equippedGlovesText: Text
    private var equippedBootsText: Text
    private var equippedChestText: Text
    private var equippedLegsText: Text

    private var itemButtons = ArrayList<ToggleButton>()
    private var equipButton = Button()
    private var dropButton = Button()
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
    private var statContainer: Container
    private var lastButtonClicked: ToggleButton? = null
    private var selectedItem: Item = ItemNone

    private var healthText: Text
    private var staminaText: Text
    private var strengthText: Text
    private var agilityText: Text
    private var luckText: Text

    init {
        container = gui.newContainer(startX, startY, 500.0f, 500.0f)
        container.background = true
        container.visible = false
        equipButton.x = 230.0f
        equipButton.y = 145.0f
        equipButton.w = 100.0f
        equipButton.h = 20.0f
        equipButton.text = "Equip"
        container.addComponent(equipButton)

        dropButton.x = 335.0f
        dropButton.y = 145.0f
        dropButton.w = 100.0f
        dropButton.h = 20.0f
        dropButton.text = "Drop"
        container.addComponent(dropButton)

        statContainer = gui.newContainer(0.0f, 720.0f - 120.0f, 100.0f, 120.0f)
        statContainer.visible = true

        healthText = statContainer.addText("Health: ${player.health}", 0.0f, 0.0f, background = true)
        staminaText = statContainer.addText("Stamina: ${player.stamina}", 0.0f, 20.0f, background = true)
        strengthText = statContainer.addText("Strength: ${player.strength}", 0.0f, 40.0f, background = true)
        agilityText = statContainer.addText("Agility: ${player.agility}", 0.0f, 60.0f, background = true)
        luckText = statContainer.addText("Luck: ${player.luck}", 0.0f, 80.0f, background = true)

        equippedWeaponText = container.addText("Weapon: ${equippedWeapon.name}", 230.0f, 175.0f, background = true)
        equippedHeadText = container.addText("Head: ${equippedHead.name}", 230.0f, 195.0f, background = true)
        equippedChestText = container.addText("Chest: ${equippedChest.name}", 230.0f, 215.0f, background = true)
        equippedGlovesText = container.addText("Gloves: ${equippedGloves.name}", 230.0f, 235.0f, background = true)
        equippedBootsText = container.addText("Boots: ${equippedBoots.name}", 230.0f, 255.0f, background = true)
        equippedLegsText = container.addText("Legs: ${equippedLegs.name}", 230.0f, 275.0f, background = true)
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
            updateEquippedItems()
        }
        else if(dropButton.active) {
            if (selectedItem != ItemNone) {
                if (selectedItem == equippedWeapon) {
                    equippedWeapon = ItemNone
                }
                else if (selectedItem == equippedChest) {
                    equippedChest = ItemNone
                }
                else if (selectedItem == equippedLegs) {
                    equippedLegs = ItemNone
                }
                else if (selectedItem == equippedBoots) {
                    equippedBoots = ItemNone
                }
                else if (selectedItem == equippedGloves) {
                    equippedGloves = ItemNone
                }
                else if (selectedItem == equippedHead) {
                    equippedHead = ItemNone
                }

                val index = items.indexOf(selectedItem)
                if (index >= 0) {
                    val button = itemButtons[index]
                    container.removeComponent(button)
                    itemButtons.removeAt(index)
                    items.remove(selectedItem)
                }

                selectedItem = ItemNone

                for (i in 0 until itemButtons.size) {
                    itemButtons[i].y = 20.0f + i*25.0f
                }
                container.isDirty = true

                updateEquippedItems()
            }
        }
    }

    fun updateEquippedItems() {
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

        statContainer.removeText(healthText)
        statContainer.removeText(staminaText)
        statContainer.removeText(strengthText)
        statContainer.removeText(agilityText)
        statContainer.removeText(luckText)

        equippedWeaponText = container.addText("Weapon: ${equippedWeapon.name}", 230.0f, 175.0f, background = true)
        equippedHeadText = container.addText("Head: ${equippedHead.name}", 230.0f, 195.0f, background = true)
        equippedChestText = container.addText("Chest: ${equippedChest.name}", 230.0f, 215.0f, background = true)
        equippedGlovesText = container.addText("Gloves: ${equippedGloves.name}", 230.0f, 235.0f, background = true)
        equippedBootsText = container.addText("Boots: ${equippedBoots.name}", 230.0f, 255.0f, background = true)
        equippedLegsText = container.addText("Legs: ${equippedLegs.name}", 230.0f, 275.0f, background = true)

        player.stamina = player.baseStamina + equippedWeapon.stamina + equippedHead.stamina + equippedChest.stamina + equippedGloves.stamina +
                equippedBoots.stamina + equippedLegs.stamina

        player.strength = player.baseStrength + equippedWeapon.strength + equippedHead.strength + equippedChest.strength + equippedGloves.strength +
                equippedBoots.strength + equippedLegs.strength

        player.agility = player.baseAgility + equippedWeapon.agility + equippedHead.agility + equippedChest.agility + equippedGloves.agility +
                equippedBoots.agility + equippedLegs.agility

        player.luck = player.baseLuck + equippedWeapon.luck + equippedHead.luck + equippedChest.luck + equippedGloves.luck +
                equippedBoots.luck + equippedLegs.luck

        player.health = player.baseHealth - player.healthDamaged + (player.stamina * 1.5f).toInt()

        healthText = statContainer.addText("Health: ${player.health}", 0.0f, 0.0f, background = true)
        staminaText = statContainer.addText("Stamina: ${player.stamina}", 0.0f, 20.0f, background = true)
        strengthText = statContainer.addText("Strength: ${player.strength}", 0.0f, 40.0f, background = true)
        agilityText = statContainer.addText("Agility: ${player.agility}", 0.0f, 60.0f, background = true)
        luckText = statContainer.addText("Luck: ${player.luck}", 0.0f, 80.0f, background = true)
    }
}
