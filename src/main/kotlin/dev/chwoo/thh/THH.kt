package dev.chwoo.thh

import dev.chwoo.thh.Game.updateInfo
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team


lateinit var plugin: JavaPlugin

var teamAlive: Team? = null
var teamDead: Team? = null
var teamKiller: Team? = null

const val CENTER_X = 261.0
const val CENTER_Y = 100.0
const val CENTER_Z = -97.0
const val GAME_DURATION = 1000 * 60 * 60 * 24 * 5
const val BAN_DURATION = 1000 * 60 * 60 * 24

////TODO: TEST!!
//const val CENTER_X = 37.0
//const val CENTER_Y = 230.0
//const val CENTER_Z = -7.0
//const val GAME_DURATION = 1000 * 60 * 60 * 2
//const val BAN_DURATION = 1000 * 60 * 30


@Suppress("unused")
class THH : JavaPlugin() {
    init {
        plugin = this
    }

    override fun onEnable() {
        SaveManager.init()

        teamAlive = createTeam("Alive", NamedTextColor.GREEN)
        teamDead = createTeam("Dead", NamedTextColor.RED)
        teamKiller = createTeam("Killer", NamedTextColor.DARK_AQUA)

        createRecipe()

        server.pluginManager.registerEvents(Events(), plugin)
        GameCommands.register()

        loop { updateInfo() }
    }

    override fun onDisable() {
        SaveManager.save()
    }

    fun createRecipe(){
        val totem = ItemStack(Material.TOTEM_OF_UNDYING, 1)
        val recipe = ShapedRecipe(NamespacedKey(plugin, "totem"),totem)

        recipe.shape("EAE", "GHG", " C ")

        recipe.setIngredient('E', Material.EMERALD_BLOCK)
        recipe.setIngredient('A', Material.GOLDEN_APPLE)
        recipe.setIngredient('G', Material.GOLD_BLOCK)
        recipe.setIngredient('H', Material.HEART_OF_THE_SEA)
        recipe.setIngredient('C', Material.GOLDEN_CARROT)

        server.addRecipe(recipe)
    }

    fun createTeam(name: String, color: NamedTextColor): Team {
        val scoreboard = server.scoreboardManager.mainScoreboard
        val t = scoreboard.getTeam(name)
        if (t != null) return t
        val team = scoreboard.registerNewTeam(name)
        team.color(color)
        return team
    }
}
