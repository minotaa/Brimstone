package club.malvaceae.malloy.features

import club.malvaceae.malloy.Malloy
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class CombatTag(val player: Player) : BukkitRunnable() {
    var timer = 30
    override fun run() {
        timer -= 1
        if (timer == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>You are free to log out."))
            cancel()
            CombatTagListener.tags[player.uniqueId] = null
        }
    }
}

class CombatTagListener : Listener {
    companion object {
        val tags = HashMap<UUID, CombatTag?>()
        val toDie = arrayListOf<UUID>()
    }

    @EventHandler
    fun onPlayerCombatQuit(e: PlayerQuitEvent) {
        if (tags[e.player.uniqueId] != null) {
            toDie.add(e.player.uniqueId)
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<red><yellow>${e.player.name}</yellow> will die the next time they log on."))
        }
    }

    @EventHandler
    fun onPlayerCombatJoin(e: PlayerJoinEvent) {
        if (toDie.contains(e.player.uniqueId)) {
            Schedulers.sync().runLater(Runnable {
                toDie.remove(e.player.uniqueId)
                e.player.damage(e.player.health * 800)
            }, 1000L)
        }
    }

    @EventHandler
    fun onPlayerDamageByPlayer(e: EntityDamageByEntityEvent) {
        if (e.damager is Player && e.entity is Player) {
            if (tags[e.entity.uniqueId] != null) {
                tags[e.entity.uniqueId]!!.timer = 30
            } else {
                tags[e.entity.uniqueId] = CombatTag(e.entity as Player)
                tags[e.entity.uniqueId]!!.runTaskTimer(Malloy.instance, 0L, 20L)
                (e.entity as Player).sendMessage(MiniMessage.miniMessage().deserialize("<red>You've been tagged for 30s! Don't log out!"))
            }
            if (tags[e.damager.uniqueId] != null) {
                tags[e.damager.uniqueId]!!.timer = 30
            } else {
                tags[e.damager.uniqueId] = CombatTag(e.damager as Player)
                tags[e.damager.uniqueId]!!.runTaskTimer(Malloy.instance, 0L, 20L)
                (e.damager as Player).sendMessage(MiniMessage.miniMessage().deserialize("<red>You've been tagged for 30s! Don't log out!"))
            }
        }
        if (e.entity is Player && e.damager is Arrow && (e.damager as Arrow).shooter is Player) {
            if (tags[e.entity.uniqueId] != null) {
                tags[e.entity.uniqueId]!!.timer = 30
            } else {
                tags[e.entity.uniqueId] = CombatTag(e.entity as Player)
                tags[e.entity.uniqueId]!!.runTaskTimer(Malloy.instance, 0L, 20L)
                (e.entity as Player).sendMessage(MiniMessage.miniMessage().deserialize("<red>You've been tagged for 30s! Don't log out!"))
            }
            if (tags[((e.damager as Arrow).shooter as Player).uniqueId] != null) {
                tags[((e.damager as Arrow).shooter as Player).uniqueId]!!.timer = 30
            } else {
                tags[((e.damager as Arrow).shooter as Player).uniqueId] = CombatTag(((e.damager as Arrow).shooter as Player))
                tags[((e.damager as Arrow).shooter as Player).uniqueId]!!.runTaskTimer(Malloy.instance, 0L, 20L)
                ((e.damager as Arrow).shooter as Player).sendMessage(MiniMessage.miniMessage().deserialize("<red>You've been tagged for 30s! Don't log out!"))
            }
        }
    }
}