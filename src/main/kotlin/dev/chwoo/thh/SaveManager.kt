package dev.chwoo.thh

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

object SaveManager {

    fun init() {
        val file = File(plugin.dataFolder, "data.yml")
        if (!file.exists()) return

        val config = YamlConfiguration.loadConfiguration(file)

        Game.alertTimes = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 60, 600)
        Game.endTime = config.getLong("endTime", 0)
        Game.playing = true

        config.getConfigurationSection("players")?.getKeys(false)?.forEach {
            val section = config.getConfigurationSection("players.${it}") ?: return@forEach
            Game.players.add(
                GamePlayer(
                    section.getString("name") ?: "???",
                    UUID.fromString(it),
                    section.getInt("score"),
                    section.getBoolean("alive"),
                    section.getBoolean("died"),
                    section.getBoolean("killed"),
                    section.getBoolean("pvp")
                )
            )
        }

        Files.move(
            file.toPath(),
            File(file.parent, "backup.yml").toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }


    fun save() {
        if (!Game.playing) return
        val config = YamlConfiguration()

        config.set("endTime", Game.endTime)

        Game.players.forEach {
            val section = config.createSection("players.${it.uuid}")
            section.set("name", it.name)
            section.set("score", it.score)
            section.set("alive", it.alive)
            section.set("died", it.died)
            section.set("killed", it.killed)
            section.set("pvp", it.pvp)
        }

        plugin.dataFolder.mkdirs()
        val file = File(plugin.dataFolder, "data.yml")
        if (!file.exists()) file.createNewFile()
        config.save(file)
    }

}