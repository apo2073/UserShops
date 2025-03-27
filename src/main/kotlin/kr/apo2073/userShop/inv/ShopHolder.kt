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
import java.util.*

class ShopHolder: InventoryHolder, Listener {
    constructor(player: Player) {
        this.player=player
    }
    constructor()

    private lateinit var player: Player
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
        if (!::player.isInitialized) return inv
        inv.contents= UserData(player).getPage(1).toTypedArray()

        inv.setItem(45,
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 1)
                meta.persistentDataContainer.set(playerKey, PersistentDataType.STRING,
                    player.uniqueId.toString())
                meta.lore(mutableListOf(Component.text("[ 페이지 ${
                    meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)
                } ]")))
                itemMeta=meta
            })
        inv.setItem(53,
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                val meta=this.itemMeta
                meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 2)
                meta.lore(mutableListOf(Component.text("[ 페이지 ${
                    meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)
                } ]")))
                itemMeta=meta
            })
        return inv
    }
    @EventHandler
    fun InventoryClickEvent.changePage() {
        if (inventory.holder !is ShopHolder) return
        isCancelled = true

        val currentItem = currentItem ?: return
        val newPage = currentItem.itemMeta?.persistentDataContainer?.get(
            pageKey, PersistentDataType.INTEGER
        ) ?: return
        if (newPage < 1) return

        val player = whoClicked as Player
        val ownerUUID = inventory.getItem(45)?.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING)
        val owner = Bukkit.getPlayer(UUID.fromString(
            ownerUUID ?: return
        )) ?: return

        val userData = UserData(owner)
        val pageItems = userData.getPage(newPage)
        if (pageItems.isEmpty()) return

        inventory.contents = pageItems.toTypedArray()

        inventory.setItem(45,
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, newPage - 1)
                meta.persistentDataContainer.set(playerKey, PersistentDataType.STRING, owner.uniqueId.toString())
                meta.lore(mutableListOf(Component.text("[ 페이지 ${
                    meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)
                } ]")))
                itemMeta = meta
            })
        inventory.setItem(53,
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, newPage + 1)
                meta.lore(mutableListOf(Component.text("[ 페이지 ${
                    meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)
                } ]")))
                itemMeta = meta
            })

        player.closeInventory()
        player.openInventory(inventory)
    }


    @EventHandler
    fun InventoryClickEvent.manage() {
        if (inventory.holder !is ShopHolder) return
        isCancelled=true

        val player =whoClicked as Player
        val owner=Bukkit.getPlayer(
            UUID.fromString(
                inventory.getItem(45)!!.itemMeta?.persistentDataContainer
                    ?.get(playerKey, PersistentDataType.STRING).toString()
            )
        ) ?: return
        if (player!=owner) return

        val page= inventory.getItem(45)!!.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.INTEGER)?:return
        if (!isRightClick) return
        player.inventory.addItem(currentItem?.apply {
            val meta=itemMeta
            val lore=meta.lore()!!
            lore[0] = null
            lore[1]=null
            meta.lore(lore)
            itemMeta=meta
        } ?: return)
        inventory.setItem(slot, null)
        UserData(player).setItemInSlot(page, slot, null)
        player.sendMessage(prefix.append(Component.text("해당 아이템을 판매 취소했습니다")))
        player.updateInventory()
    }

    val prefix = MiniMessage.miniMessage().deserialize("<b><gradient:#DBCDF0:#8962C3>[ 계시판 ]</gradient></b> ")
    @EventHandler
    fun InventoryClickEvent.bought() {
        if (inventory.holder !is ShopHolder) return
        isCancelled=true

        val player =whoClicked as Player
        val owner=Bukkit.getPlayer(
            UUID.fromString(
                inventory.getItem(45)!!.itemMeta?.persistentDataContainer?.get(
                    playerKey, PersistentDataType.STRING).toString()
            )
        ) ?: return
        val price=currentItem?.itemMeta?.persistentDataContainer?.get(
            priceKey,
            PersistentDataType.DOUBLE
        ) ?: return
        val page= inventory.getItem(45)!!.itemMeta?.persistentDataContainer?.get(
            pageKey, PersistentDataType.INTEGER
        ) ?:return

        if (!UserShop.economy.has(player, price)) {
            player.sendMessage(prefix.append(Component.text("충분한 돈을 갖고 있지 않습니다")))
            return
        }

        val userData=UserData(owner)

        player.inventory.addItem(currentItem?.apply {
            val meta=itemMeta
            val lore=meta.lore()!!
            lore[0] = null
            lore[1]= null
            meta.lore(lore)
            itemMeta=meta
        } ?: return)
        inventory.setItem(slot, null)
        player.updateInventory()
        userData.setItemInSlot(page, slot, null)
        userData.addEarnings(price.toInt())
        UserShop.economy.withdrawPlayer(player, price)
        player.sendMessage(prefix.append(Component.text("해당 아이템을 구매했습니다")))
    }
}