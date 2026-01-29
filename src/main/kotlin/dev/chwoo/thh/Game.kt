package dev.chwoo.thh

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.`object`.ObjectContents
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import kotlin.math.abs
import kotlin.math.ceil


object Game {


    var prePlaying = false
    var playing = false

    val players = mutableListOf<GamePlayer>()
    var endTime: Long = 0

    var alertTimes = mutableListOf<Int>()

    const val CENTER_X = 261.0
    const val CENTER_Z = -97.0

    fun start() {
        prePlaying = true
        alertTimes = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 60, 600)

        Bukkit.getOnlinePlayers().forEach { it.scoreboard = Bukkit.getScoreboardManager().newScoreboard }

        broadcastTitle(
            Title.title(
                "준비하세요!".comp(0x1bf28e).decorate(TextDecoration.BOLD),
                "잠시 후 게임이 시작됩니다...".comp().decorate(TextDecoration.ITALIC),
                5,
                100,
                0
            )
        )
        broadcastSound(Sound.BLOCK_CONDUIT_DEACTIVATE, 2f)

        for (i in 0..2) {
            delay((20 * 3 + i * 20).toLong()) {
                broadcastTitle(
                    Title.title(
                        "${3 - i}".comp(0xf0463a).decorate(TextDecoration.BOLD),
                        "".comp(),
                        0,
                        100,
                        0
                    )
                )
                broadcastSound(Sound.UI_BUTTON_CLICK)
            }
        }

        delay((20 * 6).toLong()) {
            broadcastTitle(
                Title.title(
                    "시작!".comp(0x1bf28e).decorate(TextDecoration.BOLD),
                    "".comp(),
                    0,
                    40,
                    10
                )
            )
            broadcastSound(Sound.ITEM_TRIDENT_RIPTIDE_1)

            Bukkit.broadcast(
                comps(
                    "3HH SMP".comp(0xfc3b19),
                    " 시작!".comp(NamedTextColor.GREEN),
                    Component.newline(),
                    "발전 과제를 달성해서 점수를 얻으세요!".comp()
                )
            )

            startGame()
        }
    }

    private fun startGame() {
        prePlaying = false
        playing = true
        players.clear()
        endTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 5
        setPlatform(Material.AIR)

        Bukkit.getWorlds().forEach {
            it.setGameRule(GameRules.ADVANCE_TIME, true)
            it.setGameRule(GameRules.ADVANCE_WEATHER, true)
            it.setGameRule(GameRules.RESPAWN_RADIUS, 1000)
        }

        Bukkit.getOnlinePlayers().forEach {
            addPlayer(it)
            it.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 8, 0, true, false, true))
        }
    }

    fun init() {
        val mainWorld = Bukkit.getWorlds().first()
        mainWorld.spawnLocation = Location(mainWorld, CENTER_X, 100.0, CENTER_Z)
        Bukkit.getWorlds().forEach {
            it.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
            it.setGameRule(GameRules.LOCATOR_BAR, false)
            it.setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false)
            it.setGameRule(GameRules.RESPAWN_RADIUS, 0)
            it.setGameRule(GameRules.ADVANCE_TIME, false)
            it.setGameRule(GameRules.ADVANCE_WEATHER, false)
            it.difficulty = Difficulty.HARD
        }
//        mainWorld.worldBorder.setCenter(centerX, centerZ)
//        mainWorld.worldBorder.size = 50.0

        setPlatform(Material.BARRIER)

        Bukkit.getOnlinePlayers().forEach {
            it.teleport(mainWorld.spawnLocation.apply { y = 100.0 })
            it.gameMode = GameMode.ADVENTURE
        }
    }

    fun stop() {
        players.forEach { it.updateScoreboard() }
        playing = false

        players.filter { !it.died }.forEach { it.score += 3 }

        val winner = players.groupBy { it.score }.entries.maxByOrNull { it.key }?.value
        if (winner != null) {
            broadcastTitle(
                Title.title(
                    Component.join(
                        JoinConfiguration.separator("   ".comp()), winner.map {
                            Component.`object`().contents(
                                ObjectContents.playerHead(it.uuid)
                            ).shadowColor(ShadowColor.none())
                        }),
                    winner.joinToString("   ") { it.name }.comp(NamedTextColor.YELLOW),
                    5, 200, 20
                )
            )
        }
        broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE)
        Bukkit.getOnlinePlayers().forEach { it.gameMode = GameMode.SPECTATOR }


        val rankedList = players.groupBy { it.score }.entries.sortedByDescending { it.key }
            .flatMapIndexed { groupIndex, (_, group) ->
                val rank = groupIndex + 1
                group.map { p -> p to rank }
            }
            .sortedByDescending { it.first.score }

        val n = 5


        delay {

            Bukkit.getOnlinePlayers().forEach { p ->
                val self = rankedList.find { it.first == getPlayer(p) } ?: return@forEach

                p.sendMessage(
                    comps(
                        Component.newline(),
                        "게임 종료!".comp(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                        Component.newline(),
                        Component.newline(),
                        self.first.score.comp(NamedTextColor.GREEN),
                        "점으로 ".comp(),
                        self.second.comp(NamedTextColor.GREEN),
                        "등 하셨습니다!".comp(),
                        Component.newline(),
                        Component.newline(),
                        Component.join(
                            JoinConfiguration.newlines(),
                            rankedList.take(n).map {
                                val col = if (it.first == getPlayer(p)) NamedTextColor.GOLD else NamedTextColor.AQUA
                                comps(
                                    it.second.comp(),
                                    ". ".comp(),
                                    it.first.name.comp(col),
                                    ": ".comp(),
                                    it.first.score.comp(NamedTextColor.GREEN)
                                )
                            }
                        ),
                        Component.newline(),
                    )
                )

            }
        }


    }

    fun setPlatform(material: Material) {
        val mainWorld = Bukkit.getWorlds().first()
        for (x in -4..4) {
            for (y in -1..2) {
                for (z in -4..4) {
                    if (y != -1 && !(abs(x) == 4 || abs(z) == 4)) continue
                    mainWorld.getBlockAt(CENTER_X.toInt() + x, 100 + y, CENTER_Z.toInt() + z).type = material
                }
            }
        }
    }


    fun addPlayer(player: Player) {
        Bukkit.advancementIterator().forEach { adv ->
            player.getAdvancementProgress(adv).let {
                it.awardedCriteria.forEach { c -> it.revokeCriteria(c) }
            }
        }
        player.joinTeam(teamAlive)
        player.apply {
            getAttribute(Attribute.MAX_HEALTH)?.baseValue = 6.0
            health = 6.0
            clearActivePotionEffects()
            totalExperience = 0
            exp = 0f
            level = 0
            inventory.clear()
            giveSpawnEffects(this)
            gameMode = GameMode.SURVIVAL
        }


        val gamePlayer = GamePlayer(player.name, player.uniqueId, 0)
        players.add(gamePlayer)
    }

    fun giveSpawnEffects(player: Player) {
        player.apply {
            foodLevel = 20
            saturation = 20f
            exhaustion = 0f
            addPotionEffects(
                listOf(
                    PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 60 * 3,
                        2,
                        false,
                        false,
                        true
                    ),
                    PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 60,
                        4,
                        true,
                        false,
                        true
                    ),
                    PotionEffect(
                        PotionEffectType.ABSORPTION,
                        20 * 60 * 10,
                        0,
                        true,
                        false,
                        true
                    )
                )
            )
        }
    }

    fun updateInfo() {
        if (!playing) return
        players.forEach { it.updateScoreboard() }
        val timeDiff = endTime - System.currentTimeMillis()
        if (timeDiff <= 1000 * 60 * 10 && alertTimes.contains(600)) {
            broadcastTitle(Title.title("".comp(), "10분 남았습니다!".comp(NamedTextColor.GRAY), 0, 100, 0))
            broadcastSound(Sound.UI_BUTTON_CLICK)
            Bukkit.broadcast("10분 남았습니다!".comp(NamedTextColor.YELLOW))
            alertTimes.remove(600)
        }
        if (timeDiff <= 1000 * 60 && alertTimes.contains(60)) {
            broadcastTitle(Title.title("".comp(), "1분 남았습니다!".comp(NamedTextColor.RED), 0, 100, 0))
            broadcastSound(Sound.UI_BUTTON_CLICK)
            Bukkit.broadcast("1분 남았습니다!".comp(NamedTextColor.YELLOW))
            alertTimes.remove(60)
        }
        if (timeDiff <= 1000 * 10) {
            val time = ceil(timeDiff.toDouble() / 1000).toInt()
            if (alertTimes.contains(time)) {
                broadcastTitle(Title.title(time.comp(NamedTextColor.RED), "".comp(), 0, 100, 0))
                broadcastSound(Sound.UI_BUTTON_CLICK)
                alertTimes.remove(time)
            }
        }
        if (timeDiff <= 0) stop()
    }

    fun getPlayer(player: Player): GamePlayer? {
        return players.find { it.uuid == player.uniqueId }
    }

    fun getTimeLeft(): String {
        val timeDiff = endTime - System.currentTimeMillis()

        val d = Duration.ofMillis(timeDiff)
        val days = d.toDays()
        val hours = d.toHoursPart()
        val minutes = d.toMinutesPart()
        val seconds = d.toSecondsPart()
        val milliseconds = d.toMillisPart() / 100

        return when {
            days > 0 -> "${days}일 ${hours}시간"
            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            timeDiff <= 0 -> "§4§l0.0초"
            else -> "§c§l${seconds}.${milliseconds}초"
        }
    }

//    fun setPvp(value: Boolean, silent: Boolean = false) {
//        Bukkit.getWorlds().forEach { it.setGameRule(GameRules.PVP, value) }
//        if (silent) return
//        if (value) {
//            broadcastTitle(
//                Title.title(
//                    "PVP".comp(0xff0000),
//                    "ON".comp(NamedTextColor.RED),
//                    10,
//                    200,
//                    10
//                )
//            )
//            broadcastSound(Sound.ENTITY_WITHER_SPAWN, 0.9f)
//            Bukkit.broadcast("PVP가 켜졌습니다!".comp(NamedTextColor.RED))
//        } else {
//            broadcastTitle(
//                Title.title(
//                    "PVP".comp(0xb8b8b8),
//                    "OFF".comp(NamedTextColor.GREEN),
//                    10,
//                    200,
//                    10
//                )
//            )
//            broadcastSound(Sound.ITEM_MACE_SMASH_AIR, 0.5f)
//            Bukkit.broadcast("PVP가 꺼졌습니다".comp(NamedTextColor.GREEN))
//        }
//    }

}