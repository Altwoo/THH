package dev.chwoo.thh

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

fun Any.comp(): Component = Component.text(this.toString())

fun Any.comp(color: TextColor): Component = this.comp().color(color)

fun Any.comp(color: Int): Component = this.comp().color(TextColor.color(color))

fun comps(vararg component: Component): Component = Component.join(
    JoinConfiguration.noSeparators(),
    *component
)

fun delay(ticks: Long = 0, function: () -> Unit) {
    Bukkit.getScheduler().runTaskLater(plugin, Runnable { function() }, ticks)
}

fun loop(interval: Long = 0, delay: Long = 0, function: () -> Unit) {
    Bukkit.getScheduler().runTaskTimer(plugin, Runnable { function() }, delay, interval)
}

fun broadcastTitle(title: Title) {
    Bukkit.getOnlinePlayers().forEach { it.showTitle(title) }
}

fun broadcastSound(sound: Sound, pitch: Float = 1f) {
    Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, sound, 10f, pitch) }
}

fun Player.joinTeam(team: Team?) {
    team ?: return
    team.addPlayer(this)
    this.playerListName(this.displayName().color(team.color()))
}