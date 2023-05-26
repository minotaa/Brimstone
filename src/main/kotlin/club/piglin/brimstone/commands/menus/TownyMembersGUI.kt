package club.piglin.brimstone.commands.menus

import club.piglin.brimstone.Brimstone
import club.piglin.brimstone.database.towns.Member
import club.piglin.brimstone.menus.Button
import club.piglin.brimstone.menus.pagination.PaginatedMenu
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class TownyMembersGUI : PaginatedMenu() {
    override fun getPrePaginatedTitle(var1: Player?): String {
        return "Town Members"
    }

    override fun getAllPagesButtons(var1: Player?): Map<Int, Button> {
        if (var1 != null) {
            val town = Brimstone.instance.townHandler.getPlayerTown(var1) ?: throw Error("This player must have a town.")
            val buttons: HashMap<Int, Button> = hashMapOf()
            val count = AtomicInteger(0)
            val members = ArrayList<Member>()
            town.members.forEach { member ->
                town.getMember(member["uniqueId"] as UUID)?.let { members.add(it) }
            }
            members.forEach {
                val m = Bukkit.getOfflinePlayer(it.uniqueId)
                buttons[count.get()] = object : Button() {
                    override fun getButtonItem(player: Player?): ItemStack {

                    }

                    override fun getDescription(var1: Player?): List<String>? {
                        return null
                    }

                    override fun getName(var1: Player?): String? {
                        return null
                    }
                }
            }
            return buttons
        }
        throw Error("A player is required for this menu.")
    }
}