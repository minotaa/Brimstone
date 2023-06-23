package club.piglin.brimstone.commands

import club.piglin.brimstone.Brimstone
import club.piglin.brimstone.utils.Chat
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.math.round
import kotlin.random.Random

class WildernessMovementCheck : Listener {
    @EventHandler
    fun onPlayerMove(e: PlayerMoveEvent) {
        if (e.hasExplicitlyChangedBlock()) {
            if (WildernessCommand.tasks[e.player.uniqueId] != null) {
                WildernessCommand.tasks[e.player.uniqueId]!!.cancel()
                WildernessCommand.tasks[e.player.uniqueId] = null
                Chat.sendComponent(e.player, "<dark_green>[Wilderness]</dark_green> <red>You moved! Cancelling teleportation.</red>")
            }
        }
    }
}

class WildernessTask(val player: Player) : BukkitRunnable(), Listener {
    var timer = 5

    override fun run() {
        player.sendActionBar(MiniMessage.miniMessage().deserialize("<dark_green>[Wilderness]</dark_green> Teleporting in <green>${timer}s</green>..."))
        timer--
        if (timer == 0) {
            cancel()
            WildernessCommand.tasks[player.uniqueId] = null
            val world = player.world
            var finalLocation: Location? = null
            while (finalLocation == null) {
                val location = Location(
                    player.world,
                    Random.nextDouble(player.location.x - 1000, player.location.x + 1000),
                    256.0,
                    Random.nextDouble(player.location.z - 1000, player.location.z + 1000)
                )
                if (world.getHighestBlockAt(location).type != Material.CACTUS &&
                    world.getHighestBlockAt(location).type != Material.LAVA &&
                    world.getHighestBlockAt(location).type != Material.WATER &&
                    world.getHighestBlockAt(location).type != Material.LILY_PAD &&
                    world.getHighestBlockAt(location).type != Material.AIR &&
                    world.worldBorder.isInside(location)
                ) {
                    finalLocation = Location(
                        world,
                        location.x,
                        world.getHighestBlockAt(location).location.y + 3,
                        location.z
                    )
                }
            }
            world.loadChunk(finalLocation.chunk)
            player.teleport(finalLocation)
            Chat.sendComponent(player, "<dark_green>[Wilderness]</dark_green> <reset>Teleported you to <green>X: ${round(finalLocation.x)}, Z: ${round(finalLocation.z)}</green>.")
        }
    }
}

class WildernessCommand : CommandExecutor {
    companion object {
        val tasks = hashMapOf<UUID, WildernessTask?>()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("You can't use this command as you aren't a player.")
            return false
        }
        val task = WildernessTask(sender)
        tasks[sender.uniqueId] = task
        tasks[sender.uniqueId]!!.runTaskTimer(Brimstone.instance, 0L, 20L)
        Chat.sendComponent(sender, "<dark_green>[Wilderness]</dark_green> Teleporting you to a random location in 5 seconds, don't move...")
        return true
    }
}