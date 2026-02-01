package dev.chwoo.thh

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import io.papermc.paper.advancement.AdvancementDisplay
import io.papermc.paper.ban.BanListType
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration
import kotlin.random.Random
import kotlin.random.nextInt

class Events : Listener {

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player
        if (!Game.playing) {
            if (player.isOp) return
            player.gameMode = GameMode.ADVENTURE
            return
        }
        val team = player.scoreboard.getPlayerTeam(player)
        player.playerListName(player.displayName().color(team?.color()))
        val gamePlayer = Game.getPlayer(player)
        gamePlayer?.name = player.name
        if (gamePlayer != null) {
            if (gamePlayer.alive) return
            player.gameMode = GameMode.SURVIVAL
            Game.giveSpawnEffects(player)
            player.addPotionEffects(
                listOf(
                    PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        20 * 15,
                        0,
                        true,
                        false,
                        true
                    ), PotionEffect(
                        PotionEffectType.DOLPHINS_GRACE,
                        20 * 60,
                        0,
                        true,
                        false,
                        true
                    )
                )
            )

            gamePlayer.alive = true
            if (gamePlayer.killed) player.joinTeam(teamKiller)
            else player.joinTeam(teamAlive)
        } else Game.addPlayer(player)
    }

    @EventHandler
    fun onRespawn(e: PlayerRespawnEvent) {
        if (!Game.playing) return
        if (e.respawnReason != PlayerRespawnEvent.RespawnReason.DEATH) return
        val world = Bukkit.getWorlds().first()
        val x = Random.nextInt(-500..500)
        val z = Random.nextInt(-500..500)
        val y = world.getHighestBlockAt(x, z).y + 75
        e.respawnLocation = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }



    @EventHandler
    fun onAdvancement(e: PlayerAdvancementDoneEvent) {
        if (e.advancement.display?.doesAnnounceToChat() != true) return
        val player = e.player
        val gamePlayer = Game.getPlayer(player) ?: return
        val dragonEgg = e.advancement.key.value() == "end/dragon_egg"
        if (dragonEgg || e.advancement.display?.frame() == AdvancementDisplay.Frame.CHALLENGE) gamePlayer.score += 3
        else gamePlayer.score++
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 10f, 2f)
        player.sendActionBar("발전 과제 달성!".comp(0x68ff4d).decorate(TextDecoration.BOLD))

        if (dragonEgg) Game.stop()
    }

    @EventHandler
    fun onCriterion(e: PlayerAdvancementCriterionGrantEvent) {
        if (Game.playing && Game.getPlayer(e.player)?.alive == true && e.player.gameMode != GameMode.SPECTATOR) return
        e.isCancelled = true
    }

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        if (!Game.playing) return
        val player = e.player
        val gamePlayer = Game.getPlayer(player) ?: return
        if (!gamePlayer.alive) return


        val killer = player.killer
        val killerGamePlayer = killer?.let { Game.getPlayer(it) }
        if (killerGamePlayer != null && killer != player) {
            if (killerGamePlayer.killed) {
                killer.killer = null
                killer.health = 0.0
                killer.joinTeam(teamDead)
                e.isCancelled = true
                player.health = player.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 6.0
                return
            } else {
                killer.playSound(killer.location, Sound.EVENT_MOB_EFFECT_RAID_OMEN, 10f, 0.9f)
                killer.showTitle(
                    Title.title(
                        "살인!".comp(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
                        "최대 체력이 1이 되었습니다!".comp(),
                        0,
                        60,
                        10
                    )
                )
                killerGamePlayer.killed = true
                killer.joinTeam(teamKiller)
                killer.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0
            }
        }


        gamePlayer.alive = false
        gamePlayer.died = true

        player.joinTeam(teamDead)


        player.respawnLocation = null

        delay {
            val banlist = Bukkit.getBanList(BanListType.PROFILE)
            banlist.addBan(player.playerProfile, "사망했습니다!", Duration.ofMillis(BAN_DURATION.toLong()), null)
            player.kick("사망했습니다!".comp(NamedTextColor.RED))
        }


        Bukkit.broadcast(
            comps(
                player.name.comp(NamedTextColor.AQUA),
                "(이)가 사망했습니다. ".comp(NamedTextColor.RED),
                "획득한 점수는 ".comp(),
                gamePlayer.score.comp(NamedTextColor.GREEN),
                "점".comp(NamedTextColor.GREEN),
                "입니다!".comp()
            )
        )

        player.showTitle(
            Title.title(
                "!사망!".comp(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
                "사망하셨습니다".comp(),
                0,
                60,
                10
            )
        )

        broadcastSound(Sound.ENTITY_WARDEN_HEARTBEAT)
        broadcastSound(Sound.ENTITY_CREAKING_DEATH)
    }


    @EventHandler
    fun onAttack(e: EntityDamageByEntityEvent) {
        if (e.entity !is Player) return
        val attacker = e.damager
        if (attacker is Player) {
            if (Game.getPlayer(attacker)?.pvp == true) return
            e.isCancelled = true
        } else if (attacker is Projectile) {
            val owner = attacker.ownerUniqueId ?: return
            if (owner == e.entity.uniqueId) return
            val gamePlayer = Game.players.find { it.uuid == owner } ?: return
            if (gamePlayer.pvp) return
            e.isCancelled = true
        }
    }


    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        val player = e.entity as? Player ?: return
        if (!Game.playing) {
            e.isCancelled = true
            return
        }
        val gamePlayer = Game.getPlayer(player) ?: return
        if (!gamePlayer.alive) {
            e.isCancelled = true
            return
        }
        if (e.finalDamage == 0.0) return

        delay {
            if (player.isDead) return@delay
            if (player.health + player.absorptionAmount >= 1.0) return@delay
            if (gamePlayer.criticalUntil > System.currentTimeMillis()) return@delay
            gamePlayer.criticalUntil = System.currentTimeMillis() + 1000 * 10
            Bukkit.broadcast("${player.name}이(가) 위태로운 상태입니다!".comp(0xd1d1d1))
            broadcastSound(Sound.ENTITY_WARDEN_HEARTBEAT)
            broadcastSound(Sound.ENTITY_CREAKING_ACTIVATE)
        }


    }

    @EventHandler
    fun onHeal(e: EntityRegainHealthEvent) {
        val player = e.entity as? Player ?: return
        if (!Game.playing) return
        val gamePlayer = Game.getPlayer(player) ?: return
        if (!gamePlayer.alive) return
        if (player.health + e.amount < 1) return
        if (gamePlayer.criticalUntil > System.currentTimeMillis()) return
        gamePlayer.criticalUntil = 0
    }

    @EventHandler
    fun onDragonEgg(e: BlockFromToEvent) {
        if (e.block.type != Material.DRAGON_EGG) return
        particleTask(e.toBlock)
    }

    @EventHandler
    fun onBlockPlaced(e: EntityChangeBlockEvent) {
        val entity = e.entity
        if (entity !is FallingBlock) return
        if (entity.blockData.material != Material.DRAGON_EGG) return
        particleTask(e.block)
    }

    fun particleTask(block: Block) {
        object : BukkitRunnable() {
            override fun run() {
                if (block.type != Material.DRAGON_EGG) {
                    this.cancel()
                    return
                }
                block.world.spawnParticle(Particle.END_ROD, block.location.add(0.5, 0.5, 0.5), 5, 0.05, 0.05, 0.05, 0.1)
            }
        }.runTaskTimer(plugin, 0, 1)
    }


    @EventHandler
    fun onItemDrop(e: ItemSpawnEvent) {
        if (e.entity.itemStack.type != Material.DRAGON_EGG) return
        e.entity.isGlowing = true
    }


}