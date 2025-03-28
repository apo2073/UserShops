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

    fun getPage(page: Int): MutableList<ItemStack> {
        val file = getPageFile(page)
        val config = loadConfig(file)
        val section = config.getConfigurationSection("items") ?: return mutableListOf()
        return section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()
    }

    fun addToPage(item: ItemStack, startPage: Int) {
        var currentPage = startPage
        while (currentPage < 1000) {
            val file = getPageFile(currentPage)
            val config = loadConfig(file)
            val section = config.getConfigurationSection("items") ?: config.createSection("items")
            val items = section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()

            if (items.size < 45) {
                section.set(items.size.toString(), item)
                saveConfig(file, config)
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
        section.set(slot.toString(), item)
        saveConfig(file, config)
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
}