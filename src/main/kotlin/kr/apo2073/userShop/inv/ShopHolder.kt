package kr.apo2073.userShop.inv

import kr.apo2073.userShop.UserShop
import kr.apo2073.userShop.utilities.UserData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*

class ShopHolder : InventoryHolder, Listener {
    constructor(player: Player) {
        this.player = player
    }
    constructor()

    private lateinit var player: Player
    private val plugin = UserShop.plugin
    private val inv = Bukkit.createInventory(
        this,
        9 * 6,
        Component.text(plugin.config.getString("거래소.타이틀") ?: "${if (::player.isInitialized) player.name else "Unknown"}의 게시판")
    )
    private val pageKey = NamespacedKey(plugin, "page")
    private val playerKey = NamespacedKey(plugin, "player")
    private val priceKey = NamespacedKey(plugin, "price")
    val prefix = MiniMessage.miniMessage().deserialize("<b><gradient:#DBCDF0:#8962C3>[ 계시판 ]</gradient></b> ")

    override fun getInventory(): Inventory {
        if (!::player.isInitialized) return inv
        inv.contents = UserData(player).getPage(1).toTypedArray()

        inv.setItem(45,
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 1)
                meta.persistentDataContainer.set(playerKey, PersistentDataType.STRING, player.uniqueId.toString())
                meta.lore(mutableListOf(Component.text("[ 페이지 ${meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)} ]")))
                itemMeta = meta
            })
        inv.setItem(53,
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, 2)
                meta.lore(mutableListOf(Component.text("[ 페이지 ${meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)} ]")))
                itemMeta = meta
            })
        return inv
    }

    @EventHandler
    fun changePage(event: InventoryClickEvent) {
        if (event.inventory.holder !is ShopHolder) return
        event.isCancelled = true

        if (event.clickedInventory?.type==InventoryType.PLAYER) return
        val currentItem = event.currentItem ?: return
        if (currentItem.isEmpty || currentItem.type.isAir) return

        val newPage = currentItem.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.INTEGER) ?: return
        if (newPage < 1) return

        val player = event.whoClicked as Player
        val ownerUUID = event.inventory.getItem(45)?.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING) ?: return
        val owner = Bukkit.getPlayer(UUID.fromString(ownerUUID)) ?: return

        val userData = UserData(owner)
        val pageItems = userData.getPage(newPage)
        if (pageItems.isEmpty()) return

        event.inventory.contents = pageItems.toTypedArray()

        event.inventory.setItem(45,
            ItemStack(Material.RED_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("이전 페이지").color(NamedTextColor.RED))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, if (newPage != 1) newPage - 1 else 1)
                meta.persistentDataContainer.set(playerKey, PersistentDataType.STRING, owner.uniqueId.toString())
                meta.lore(mutableListOf(Component.text("[ 페이지 ${meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)} ]")))
                itemMeta = meta
            })
        event.inventory.setItem(53,
            ItemStack(Material.GREEN_STAINED_GLASS_PANE).apply {
                val meta = itemMeta
                meta.displayName(Component.text("다음 페이지").color(NamedTextColor.GREEN))
                meta.persistentDataContainer.set(pageKey, PersistentDataType.INTEGER, newPage + 1)
                meta.lore(mutableListOf(Component.text("[ 페이지 ${meta.persistentDataContainer.get(pageKey, PersistentDataType.INTEGER)} ]")))
                itemMeta = meta
            })

        player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f)
        player.closeInventory()
        player.openInventory(event.inventory)
    }

    @EventHandler
    fun manage(event: InventoryClickEvent) {
        if (event.inventory.holder !is ShopHolder) return
        event.isCancelled = true

        if (event.clickedInventory?.type == InventoryType.PLAYER) return
        val currentItem = event.currentItem ?: return
        if (currentItem.isEmpty || currentItem.type.isAir) return

        val player = event.whoClicked as Player
        val ownerUUID = event.inventory.getItem(45)?.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING)
        val owner = Bukkit.getPlayer(UUID.fromString(ownerUUID ?: return)) ?: return
        if (player != owner) return

        val page = event.inventory.getItem(45)?.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.INTEGER) ?: return
        if (!event.isRightClick) return

        if (event.slot == 45 || event.slot == 53) return
        val item = currentItem.clone().apply {
            val meta = itemMeta
            meta.persistentDataContainer.remove(priceKey)
            val lore = meta.lore() ?: mutableListOf()
            if (lore.isNotEmpty()) meta.lore(lore.drop(2).toMutableList())
            itemMeta = meta
        }

        player.inventory.addItem(item)
        event.inventory.setItem(event.slot, null)
        UserData(player).removeFromPage(page, currentItem)
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
        player.sendMessage(prefix.append(Component.text("해당 아이템을 판매 취소했습니다")))
    }

    @EventHandler
    fun bought(event: InventoryClickEvent) {
        if (event.inventory.holder !is ShopHolder) return
        event.isCancelled = true

        if (event.clickedInventory?.type==InventoryType.PLAYER) return
        val currentItem = event.currentItem ?: return
        if (currentItem.isEmpty || currentItem.type.isAir) return

        val player = event.whoClicked as Player
        val ownerUUID = event.inventory.getItem(45)?.itemMeta?.persistentDataContainer?.get(playerKey, PersistentDataType.STRING)
        val owner = Bukkit.getPlayer(UUID.fromString(ownerUUID ?: return)) ?: return
        if (player == owner) return

        if (event.slot == 45 || event.slot == 53) return
        val price = currentItem.itemMeta?.persistentDataContainer?.get(priceKey, PersistentDataType.DOUBLE) ?: return
        val page = event.inventory.getItem(45)?.itemMeta?.persistentDataContainer?.get(pageKey, PersistentDataType.INTEGER) ?: return

        if (!UserShop.economy.has(player, price)) {
            player.sendMessage(prefix.append(Component.text("충분한 돈을 갖고 있지 않습니다")))
            return
        }

        val userData = UserData(owner)
        val item = currentItem.clone().apply {
            val meta = itemMeta
            meta.persistentDataContainer.remove(priceKey)
            val lore = meta.lore() ?: mutableListOf()
            if (lore.isNotEmpty()) meta.lore(lore.drop(2).toMutableList())
            itemMeta = meta
        }

        player.inventory.addItem(item)
        event.inventory.setItem(event.slot, null)
        userData.setItemInSlot(page, event.slot, null)
        userData.addEarnings(price.toInt())
        UserShop.economy.withdrawPlayer(player, price)
        player.playSound(player.location, Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f)
        player.sendMessage(prefix.append(Component.text("해당 아이템을 구매했습니다")))
    }
}