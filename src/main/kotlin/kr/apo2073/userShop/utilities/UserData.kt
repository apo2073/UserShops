package kr.apo2073.userShop.utilities

import kr.apo2073.userShop.UserShop
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.IOException
import java.util.logging.Level

class UserData(private val player: Player) {

    private val baseFolder = File(UserShop.plugin.dataFolder, "userdata/${player.uniqueId}").apply { mkdirs() }
    private val earningsFile = File(baseFolder, "shop.yml").apply { if (!exists()) createNewFile() }

    private fun loadConfig(file: File): YamlConfiguration {
        return try {
            YamlConfiguration.loadConfiguration(file)
        } catch (e: Exception) {
            UserShop.plugin.logger.log(Level.SEVERE, "Failed to load config ${file.name} for ${player.uniqueId}", e)
            YamlConfiguration()
        }
    }

    private fun saveConfig(file: File, config: YamlConfiguration) {
        try {
            config.save(file)
        } catch (e: IOException) {
            UserShop.plugin.logger.log(Level.SEVERE, "Failed to save config ${file.name} for ${player.uniqueId}", e)
        }
    }

    private fun getPageFile(page: Int): File {
        return File(baseFolder, "page$page.yml").apply { if (!exists()) createNewFile() }
    }

    private fun serializeItemStack(item: ItemStack?): Map<String, Any>? {
        return item?.serialize()
    }

    private fun deserializeItemStack(serializedItem: Map<String, Any>?): ItemStack? {
        return if (serializedItem == null) null else ItemStack.deserialize(serializedItem)
    }

    fun getPage(page: Int): MutableList<ItemStack> {
        val config = loadConfig(getPageFile(page))
        val section = config.getConfigurationSection("items") ?: return mutableListOf()
        return section.getKeys(false).mapNotNull { deserializeItemStack(section.getConfigurationSection(it)?.getValues(false)) }.toMutableList()
    }

    fun addToPage(item: ItemStack, startPage: Int) {
        var currentPage = startPage
        while (currentPage < 1000) {
            val file = getPageFile(currentPage)
            val config = loadConfig(file)
            val section = config.getConfigurationSection("items") ?: config.createSection("items")
            val items = section.getKeys(false).mapNotNull { deserializeItemStack(section.getConfigurationSection(it)?.getValues(false)) }.toMutableList()

            if (items.size < 45) {
                section.set(items.size.toString(), serializeItemStack(item))
                config.save(file)
                return
            }
            currentPage++
        }
        UserShop.plugin.logger.warning("No available page for ${player.uniqueId} starting from $startPage")
    }

    fun setItemInSlot(page: Int, slot: Int, item: ItemStack?) {
        if (slot !in 0..44) throw IllegalArgumentException("Slot must be between 0 and 44")

        val file = getPageFile(page)
        val config = loadConfig(file)
        val section = config.getConfigurationSection("items") ?: config.createSection("items")
        section.set(slot.toString(), serializeItemStack(item))
        config.save(file)
    }

    fun addEarnings(amount: Int) {
        val config = loadConfig(earningsFile)
        val currentEarnings = getEarnings()
        config.set("board.earnings", currentEarnings + amount)
        saveConfig(earningsFile, config)
    }

    fun getEarnings(): Int {
        val config = loadConfig(earningsFile)
        return config.getInt("board.earnings", 0)
    }

    fun clearEarnings() {
        val config = loadConfig(earningsFile)
        config.set("board.earnings", 0)
        saveConfig(earningsFile, config)
    }

    fun removeFromPage(page: Int, item: ItemStack) {
        val items = getPage(page)
        val itemIndex = items.indexOfFirst { it.isSimilar(item) }
        if (itemIndex != -1) {
            items.removeAt(itemIndex)
            savePage(page, items)
        } else {
            UserShop.plugin.logger.warning("Item not found in page $page for ${player.uniqueId}")
        }
    }

    fun savePage(page: Int, items: List<ItemStack>) {
        val config = loadConfig(getPageFile(page))
        config.set("items", items.map { serializeItemStack(it) })
        try {
            config.save(getPageFile(page))
        } catch (e: IOException) {
            UserShop.plugin.logger.log(Level.SEVERE, "Failed to save items for page $page", e)
        }
    }
}