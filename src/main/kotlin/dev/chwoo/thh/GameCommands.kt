package dev.chwoo.thh

import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player


object GameCommands {

    fun register() {
        val thhCmd = Commands.literal("3HH")
            .requires { it.sender.isOp }
            .then(
                Commands.literal("start")
                    .executes { ctx ->
                        val sender = ctx.source.sender
                        if (Game.playing || Game.prePlaying) {
                            sender.sendMessage("게임이 이미 진행중입니다!".comp(NamedTextColor.RED))
                            return@executes 1
                        }
                        Game.start()
                        sender.sendMessage("게임이 시작되었습니다!".comp(NamedTextColor.GOLD))
                        return@executes 1
                    }
            )
            .then(
                Commands.literal("stop")
                    .then(
                        Commands.argument("confirm", StringArgumentType.string())
                            .executes { ctx ->
                                val sender = ctx.source.sender
                                if (!Game.playing) {
                                    sender.sendMessage("진행중인 게임이 없습니다!".comp(NamedTextColor.RED))
                                    return@executes 1
                                }
                                val confirm = ctx.getArgument("confirm", String::class.java)
                                if (confirm != "confirm") {
                                    sender.sendMessage(
                                        comps(
                                            "/3HH stop confirm".comp(NamedTextColor.YELLOW),
                                            " 으로 게임을 중단하세요".comp(NamedTextColor.RED),
                                        )
                                    )
                                    return@executes 1
                                }
                                Game.stop()
                                sender.sendMessage("게임이 중단되었습니다!".comp(NamedTextColor.GOLD))
                                return@executes 1
                            }
                    )
            ).then(
                Commands.literal("init")
                    .then(
                        Commands.argument("confirm", StringArgumentType.string())
                            .executes { ctx ->
                                val sender = ctx.source.sender
                                if (Game.playing || Game.prePlaying) {
                                    sender.sendMessage("게임이 이미 진행중입니다!".comp(NamedTextColor.RED))
                                    return@executes 1
                                }
                                val confirm = ctx.getArgument("confirm", String::class.java)
                                if (confirm != "confirm") {
                                    sender.sendMessage(
                                        comps(
                                            "/3HH init confirm".comp(NamedTextColor.YELLOW),
                                            " 으로 초기 세팅을 하세요".comp(NamedTextColor.RED),
                                        )
                                    )
                                    return@executes 1
                                }
                                Game.init()
                                sender.sendMessage("초기 세팅을 완료했습니다!".comp(NamedTextColor.GOLD))
                                return@executes 1
                            }
                    )
            ).build()

        val pvpCmd = Commands.literal("pvp")
            .executes { ctx ->
                val player = ctx.source.executor as? Player ?: return@executes 1
                val gamePlayer = Game.getPlayer(player) ?: return@executes 1
                gamePlayer.pvp = !gamePlayer.pvp

                if (gamePlayer.pvp) {
                    player.playSound(player.location, Sound.ENTITY_WITHER_SPAWN, 10f, 0.9f)
                    player.sendMessage("PVP를 켰습니다!".comp(NamedTextColor.RED))
                } else {
                    player.playSound(player.location, Sound.ITEM_MACE_SMASH_AIR, 10f, 0.5f)
                    player.sendMessage("PVP를 껐습니다".comp(NamedTextColor.GRAY))
                }
                return@executes 1
            }.build()

        val reviveCmd = Commands.literal("revive")
            .requires { it.sender.isOp }
            .then(Commands.argument("player", ArgumentTypes.player()).executes { ctx ->
                val p = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java) ?: return@executes 1
                val playerList = p.resolve(ctx.source) ?: return@executes 1
                val player = playerList.firstOrNull() ?: return@executes 1
                player.gameMode = GameMode.SURVIVAL
                val gamePlayer = Game.getPlayer(player)
                gamePlayer?.alive = true
                if (gamePlayer?.killed == true) player.joinTeam(teamKiller)
                else player.joinTeam(teamAlive)
                ctx.source.sender.sendMessage("플레이어를 부활시켰습니다.".comp(NamedTextColor.GOLD))
                return@executes 1
            })
            .build()

        val killerCmd = Commands.literal("killer")
            .requires { it.sender.isOp }
            .then(Commands.argument("player", ArgumentTypes.player()).executes { ctx ->
                val p = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java) ?: return@executes 1
                val playerList = p.resolve(ctx.source) ?: return@executes 1
                val player = playerList.firstOrNull() ?: return@executes 1

                val gamePlayer = Game.getPlayer(player)

                player.playSound(player.location, Sound.EVENT_MOB_EFFECT_RAID_OMEN, 10f, 0.9f)
                player.showTitle(
                    Title.title(
                        "살인!".comp(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
                        "최대 체력이 1이 되었습니다!".comp(),
                        0,
                        60,
                        10
                    )
                )
                gamePlayer?.killed = true
                player.joinTeam(teamKiller)
                player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0

                ctx.source.sender.sendMessage("플레이어를 살인자로 지정했습니다.".comp(NamedTextColor.GOLD))
                return@executes 1
            })
            .build()

        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            it.registrar().register(thhCmd)
            it.registrar().register(pvpCmd)
            it.registrar().register(reviveCmd)
            it.registrar().register(killerCmd)
        }
    }
}