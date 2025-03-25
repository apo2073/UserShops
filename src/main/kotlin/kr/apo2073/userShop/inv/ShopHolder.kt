package kr.apo2073.userShop.inv

import kr.apo2073.userShop.UserShop
import kr.apo2073.userShop.utilities.UserData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ShopHolder(private val player: Player): InventoryHolder, Listener {
    private val plugin=UserShop.plugin
    private val inv= Bukkit.createInventory(
        this,
        9*6,
        Component.text(plugin.config.getString("거래소.타이틀").toString())
    )
    private val pageKey=NamespacedKey(plugin, "page")
    private val playerKey=NamespacedKey(plugin, "player")
    private val priceKey=NamespacedKey(plugin, "price")

    override fun getInventory():Inventory {
        inv.contents= UserData(player).getPage(1).toTypedArray()

        inv.setItem(45,
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 1)
                meta.persistentDataContainer.set(playerKey, PersistentDataType.STRING, player.uniqueId.toString())
                itemMeta=meta
            })
        inv.setItem(53,
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 2)
                itemMeta=meta
            })
        return inv
    }

    @EventHandler
    fun InventoryClickEvent.changePage() {
        if (inventory.holder !is ShopHolder) return
        val page= currentItem?.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.INTEGER)?:return
        val player =whoClicked as Player
        val owner=Bukkit.getPlayer(currentItem?.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING).toString()) ?: return
        player.closeInventory()
        player.openInventory(inv.apply {
            contents= UserData(owner).getPage(page).toTypedArray()

            setItem(45,
                ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                    val meta=this.itemMeta
                    meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                    meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, page-1)
                    meta.persistentDataContainer.set(playerKey, PersistentDataType.STRING, owner.uniqueId.toString())
                    itemMeta=meta
                })
            setItem(53,
                ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                    val meta=this.itemMeta
                    meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                    meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, page+1)
                    itemMeta=meta
                })
        })
    }

    @EventHandler
    fun InventoryClickEvent.manage() {
        if (inventory.holder !is ShopHolder) return

        val player =whoClicked as Player
        val owner=Bukkit.getPlayer(inventory.getItem(45)!!.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING).toString()) ?: return
        if (player!=owner) return

        val page= inventory.getItem(45)!!.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.INTEGER)?:return

        if (!isRightClick) return
        player.inventory.addItem(currentItem ?: return)
        UserData(player).setItemInSlot(page, slot, null)
        player.sendMessage(prefix.append(Component.text("해당 아이템을 판매 취소했습니다")))
    }

    private val prefix = MiniMessage.miniMessage().deserialize("<b><gradient:#DBCDF0:#8962C3>[ 계시판 ]</gradient></b> ")
    @EventHandler
    fun InventoryClickEvent.bought() {
        if (inventory.holder !is ShopHolder) return

        val player =whoClicked as Player
        val owner=Bukkit.getPlayer(inventory.getItem(45)!!.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING).toString()) ?: return
        val price=currentItem?.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.DOUBLE) ?: return
        val userData=UserData(owner)
        userData.addEarnings(price.toInt())
        UserShop.economy.withdrawPlayer(player, price)
        player.sendMessage(prefix.append(Component.text("해당 아이템을 구매했습니다")))
    }
}