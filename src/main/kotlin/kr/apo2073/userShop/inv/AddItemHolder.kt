package kr.apo2073.userShop.inv

import kr.apo2073.userShop.UserShop
import kr.apo2073.userShop.utilities.UserData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class AddItemHolder: InventoryHolder, Listener {
    private val plugin=UserShop.plugin
    private val inv= Bukkit.createInventory(
        this,
        9*6,
        Component.text(plugin.config.getString("아이템추가.타이틀").toString())
    )

    override fun getInventory(): Inventory {
        var num=1
        intArrayOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 42).forEach {
            inv.setItem(it, ItemStack(Material.WHITE_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("${if (it==42) 0 else num}").color(TextColor.color(0xADB2D4)))
                meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS)
                this.itemMeta=meta
            })
            num+=1
        }
        inv.setItem(43, ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
            val meta=this.itemMeta
            meta.displayName(Component.text("완료").color(TextColor.color(0xC1CFA1)))
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS)
            this.itemMeta=meta
        })
        inv.setItem(41, ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
            val meta=this.itemMeta
            meta.displayName(Component.text("지우기").color(TextColor.color(0xD76C82)))
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS)
            this.itemMeta=meta
        })
        return inv
    }

    @EventHandler
    fun InventoryClickEvent.onClick() {
        if (inventory.holder !is AddItemHolder) return
        isCancelled=true
        val player = whoClicked as Player
        val item = inventory.getItem(10) ?: return
        val meta = item.itemMeta ?: return
        val currentLore = meta.lore() ?: mutableListOf()
        if (currentLore.isEmpty()) currentLore.add(Component.text("0"))
        val price = StringBuilder(PlainTextComponentSerializer.plainText().serialize(currentLore[0]))
        when (slot) {
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
                player.inventory.setItemInMainHand(null)
                val meta=item.itemMeta
                val lore=meta.lore() ?: mutableListOf()
                try {
                    meta.persistentDataContainer.set(NamespacedKey(UserShop.plugin, "price"),
                        PersistentDataType.DOUBLE, price.toString().toInt().toDouble()
                    )
                } catch (e:NumberFormatException) {
                    player.sendMessage(ShopHolder().prefix.append(Component.text("가격을 입력해 주세요!")))
                }
                lore[0] = Component.text("가격: ${price}원").color(TextColor.color(0xDDEB9D))
                lore.add(1, Component.text("플레이어 ${player.name}이(가) 등록함").color(TextColor.color(0xC68EFD)))
                meta.lore(lore)
                item.itemMeta=meta
                val userData = UserData(player)
                for (i in 1..1000) {
                    if (userData.getPage(i).size >= 45) continue
                    else {
                        userData.addToPage(item, i)
                        break
                    }
                }
                player.closeInventory()
                return
            }
            else -> return
        }
        currentLore[0] = Component.text(price.toString())
        meta.lore(currentLore)
        item.itemMeta = meta
        player.updateInventory()
    }
}