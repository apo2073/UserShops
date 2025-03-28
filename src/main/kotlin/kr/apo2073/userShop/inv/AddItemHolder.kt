package kr.apo2073.userShop.inv

import kr.apo2073.userShop.UserShop
import kr.apo2073.userShop.utilities.UserData
import kr.apo2073.userShop.utilities.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class AddItemHolder : InventoryHolder, Listener {
    private val plugin = UserShop.plugin
    private val inv = Bukkit.createInventory(
        this,
        9 * 6,
        Component.text(plugin.config.getString("아이템추가.타이틀") ?: "아이템 추가")
    )
    private val tempPriceKey = NamespacedKey(plugin, "temp-price")
    private val priceKey = NamespacedKey(plugin, "price")
    private val prefix = MiniMessage.miniMessage().deserialize("<b><gradient:#DBCDF0:#8962C3>[ 계시판 ]</gradient></b> ")

    override fun getInventory(): Inventory {
        var num = 1
        intArrayOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 42).forEach {
            inv.setItem(it, ItemStack(Material.WHITE_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("${if (it == 42) 0 else num}").color(TextColor.color(0xADB2D4)))
                meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS)
                itemMeta = meta
            })
            num += 1
        }
        inv.setItem(43, ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
            val meta = itemMeta
            meta.displayName(Component.text("완료").color(TextColor.color(0xC1CFA1)))
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS)
            itemMeta = meta
        })
        inv.setItem(41, ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
            val meta = itemMeta
            meta.displayName(Component.text("지우기").color(TextColor.color(0xD76C82)))
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS)
            itemMeta = meta
        })
        return inv
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is AddItemHolder) return
        event.isCancelled = true

        val player = event.whoClicked as Player
        val item = event.inventory.getItem(10) ?: return
        val meta = item.itemMeta ?: return
        val tempPrice = meta.persistentDataContainer.get(tempPriceKey, PersistentDataType.STRING) ?: "0"
        val price = StringBuilder(if (tempPrice == "0") "" else tempPrice)

        when (event.slot) {
            14 -> price.append(1)
            15 -> price.append(2)
            16 -> price.append(3)
            23 -> price.append(4)
            24 -> price.append(5)
            25 -> price.append(6)
            32 -> price.append(7)
            33 -> price.append(8)
            34 -> price.append(9)
            42 -> price.append(0)
            41 -> if (price.isNotEmpty()) price.deleteCharAt(price.length - 1)
            43 -> {
                if (price.isEmpty()) {
                    player.sendMessage(prefix.append(Component.text("가격을 입력해 주세요!")))
                    return
                }
                val finalPrice = price.toString().toDoubleOrNull()
                if (finalPrice == null) {
                    player.sendMessage(prefix.append(Component.text("유효한 가격을 입력해 주세요!")))
                    return
                }
                player.inventory.setItemInMainHand(null)
                item.apply {
                    val meta = itemMeta
                    val lore = mutableListOf(
                        Component.text("판매 가격: ").color(NamedTextColor.GRAY)
                            .append(Component.text("$finalPrice").color(TextColor.color(0xDDEB9D))),
                        Component.text("플레이어 ${player.name}이(가) 등록함").color(TextColor.color(0xC68EFD))
                    )
                    meta.lore(lore)
                    meta.persistentDataContainer.set(priceKey, PersistentDataType.DOUBLE, finalPrice)
                    meta.persistentDataContainer.remove(tempPriceKey)
                    itemMeta = meta
                }
                val userData = UserData(player)
                for (i in 1..1000) {
                    if (userData.getPage(i).size >= 45) continue
                    userData.addToPage(item, i)
                    break
                }
                player.playSound(player.location, Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f)
                player.closeInventory()
                player.sendMessage(prefix.append(Component.text("아이템 등록을 완료했습니다")))
                return
            }
            else -> return
        }

        player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f)
        val lore = plugin.config.getStringList("아이템추가.가격-설정-로어")
            .map { it.replace("{price}", price.toString()) }
            .map { it.toComponent() }
        meta.lore(lore)
        meta.persistentDataContainer.set(tempPriceKey, PersistentDataType.STRING, price.toString())
        item.itemMeta = meta
    }
}