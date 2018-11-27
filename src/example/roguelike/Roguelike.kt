package example.roguelike

import example.roguelike.Entity.Attack
import example.roguelike.Entity.HealthBar
import example.roguelike.Entity.Inventory
import example.roguelike.Entity.Player
import example.roguelike.Level.Level
import org.joml.Vector2f
import org.joml.Vector3f
import rain.Rain
import rain.api.entity.EntitySystem
import rain.api.gfx.Material
import rain.api.gfx.Texture2d
import rain.api.gfx.TextureFilter
import rain.api.gui.Container
import rain.api.gui.Text
import rain.api.scene.Camera

class Roguelike: Rain() {
    lateinit var gameState: GameState
    lateinit var menuState: MenuState

    override fun init() {
        gameState = GameState(stateManager)
        stateManager.states.put("game", gameState)

        menuState = MenuState(stateManager)
        stateManager.states.put("menu", menuState)

        stateManager.startState("game")
    }

    override fun update() {

    }
}
