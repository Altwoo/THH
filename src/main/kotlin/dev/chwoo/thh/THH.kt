package dev.chwoo.thh

import dev.chwoo.thh.Game.updateInfo
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team

lateinit var plugin: JavaPlugin

var teamAlive: Team? = null
var teamDead: Team? = null
var teamKiller: Team? = null

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

        server.pluginManager.registerEvents(Events(), plugin)
        GameCommands.register()

        loop { updateInfo() }
    }

    override fun onDisable() {
        SaveManager.save()
    }

    fun createTeam(name: String, color: NamedTextColor): Team {
        val scoreboard = server.scoreboardManager.mainScoreboard
        val t = scoreboard.getTeam(name)
        if(t != null) return t
        val team = scoreboard.registerNewTeam(name)
        team.color(color)
        return team
    }
}
