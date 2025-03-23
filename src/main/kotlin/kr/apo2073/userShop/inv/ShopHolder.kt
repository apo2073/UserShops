package kr.apo2073.userShop.inv

import kr.apo2073.userShop.UserShop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ShopHolder: InventoryHolder, Listener {
    private val plugin=UserShop.plugin
    private val inv= Bukkit.createInventory(
        this,
        9*6,
        Component.text(plugin.config.getString("거래소.타이틀").toString())
    )
    private val pageKey=NamespacedKey(plugin, "page")

    override fun getInventory():Inventory {
        inv.setItem(45,
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 1)
                itemMeta=meta
            })
        inv.setItem(53,
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                itemMeta=meta
            })
        return inv
    }

    @EventHandler
    fun InventoryClickEvent.changePage() {

    }
}