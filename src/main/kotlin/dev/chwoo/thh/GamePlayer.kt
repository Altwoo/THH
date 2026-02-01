package dev.chwoo.thh

import io.papermc.paper.scoreboard.numbers.NumberFormat
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import java.util.*

data class GamePlayer(
    var name: String,
    val uuid: UUID,
    var score: Int,
    var alive: Boolean = true,
    var died: Boolean = false,
    var killed: Boolean = false,
    var pvp: Boolean = false
) {
    private var scoreboard: Scoreboard? = null
    var criticalUntil: Long = 0

    override fun equals(other: Any?): Boolean {
        return uuid == (other as? GamePlayer)?.uuid
    }

    override fun hashCode(): Int {
        var result = score
        result = 31 * result + name.hashCode()
        result = 31 * result + uuid.hashCode()
        return result
    }

    fun updateScoreboard() {
        val player = Bukkit.getPlayer(uuid) ?: return
        if (scoreboard == null) scoreboard = Bukkit.getScoreboardManager().newScoreboard
        player.scoreboard = scoreboard!!
        val objective = scoreboard?.getObjective("3HH") ?: scoreboard?.registerNewObjective(
            "3HH",
            Criteria.DUMMY,
            "3HH SMP".comp(0xfc3b19)
        )
        objective?.displaySlot = DisplaySlot.SIDEBAR
        objective?.numberFormat(NumberFormat.blank())


        val rankedList =
            Game.players.groupBy { it.score + if (!it.died) 3 else 0 }.entries.sortedByDescending { it.key }
                .flatMapIndexed { groupIndex, (_, group) ->
                    val rank = groupIndex + 1
                    group.map { p -> p to rank }
                }

        val n = 5

        val self = rankedList.find { it.first == this }
        val excluded = if (self == null || rankedList.take(n).any { it.first == this }
        ) listOf(" ") else listOf(
            "...",
            "${self.second}. §6${if (alive) "" else "§m"}${self.first.name}§r: §a${self.first.score}${if (!died) " §2+3" else ""}",
            "  "
        )

        val lines = listOf(
            "   ",
            *rankedList.take(n).map {
                val col =
                    if (it.first == this) "§6" else if (!it.first.alive) "§c" else if (it.first.killed) "§3" else "§b"
                "${it.second}. ${col}${if (it.first.alive) "" else "§m"}${it.first.name}§r: §a${it.first.score}${if (!it.first.died) " §2+3" else ""}"
            }.toTypedArray(),
            *excluded.toTypedArray(),
            "PVP: ${if(pvp) "§cON" else "§7OFF"}",
            "     ",
            "§7§o종료까지...",
            Game.getTimeLeft(),
            "       "
        )
        scoreboard?.entries?.forEach {
            objective?.getScore(it)?.resetScore()
        }

        lines.forEachIndexed { idx, line ->
            objective?.getScore(line)?.score = lines.size - idx
        }

    }


}
