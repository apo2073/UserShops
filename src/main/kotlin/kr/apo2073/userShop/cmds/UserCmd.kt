package kr.apo2073.userShop.cmds

import kr.apo2073.userShop.UserShop
import kr.apo2073.userShop.inv.AddItemHolder
import kr.apo2073.userShop.inv.ShopHolder
import kr.apo2073.userShop.utilities.UserData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

class UserCmd : TabExecutor {
    private val plugin = UserShop.plugin
    private val prefix = MiniMessage.miniMessage().deserialize("<b><gradient:#DBCDF0:#8962C3>[ 계시판 ]</gradient></b> ")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) return false

        if (!sender.hasPermission("apo.board.cmds")) {
            sender.sendMessage(prefix.append(Component.text("명령어를 사용할 권한이 없습니다!")))
            return true
        }
        when (args.getOrNull(0)) {
            "등록" -> {
                val itemInHand = sender.inventory.itemInMainHand
                if (itemInHand.type.isAir) {
                    sender.sendMessage(prefix.append(Component.text("손에 아이템을 들어야 합니다!", NamedTextColor.RED)))
                    return true
                }
                val holder = AddItemHolder()
                sender.openInventory(holder.inventory.apply {
                    setItem(10, itemInHand.clone())
                })
            }

            "대금수령" -> {
                val userData = UserData(sender)
                val earnings = userData.getEarnings()
                if (earnings <= 0) {
                    sender.sendMessage(prefix.append(Component.text("수령할 대금이 없습니다.", NamedTextColor.YELLOW)))
                    return true
                }
                UserShop.economy.depositPlayer(sender, earnings.toDouble())
                userData.clearEarnings()
                sender.sendMessage(prefix.append(Component.text("대금 ${earnings}원을 수령했습니다.", NamedTextColor.GREEN)))
            }

            else -> {
                val targetPlayer = if (args.isEmpty()) sender else Bukkit.getPlayer(args[0])
                if (targetPlayer == null && args.isNotEmpty()) {
                    sender.sendMessage(
                        prefix.append(
                            Component.text(
                                "플레이어 '${args[0]}'을 찾을 수 없습니다.",
                                NamedTextColor.RED
                            )
                        )
                    )
                    return true
                }
                val target = targetPlayer ?: sender
                val holder = ShopHolder(target)
                sender.openInventory(holder.inventory)
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("등록", "대금수령").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0] != "등록" && args[0] != "대금수령") {
                Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            } else emptyList()
            else -> emptyList()
        }
    }
}