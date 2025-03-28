package kr.apo2073.userShop.utilities

import kr.apo2073.userShop.UserShop
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.IOException
import java.util.logging.Level

class UserData(private val player: Player) {
    private val file = File(UserShop.plugin.dataFolder, "userdata/${player.uniqueId}.yml").apply { parentFile.mkdirs() }
    private var config = loadConfig()

    private fun loadConfig(): YamlConfiguration {
        return try {
            YamlConfiguration.loadConfiguration(file)
        } catch (e: Exception) {
            UserShop.plugin.logger.log(Level.SEVERE, "Failed to load config for ${player.uniqueId}", e)
            YamlConfiguration()
        }
    }

    private fun saveConfig() {
        try {
            config.save(file)
        } catch (e: IOException) {
            UserShop.plugin.logger.log(Level.SEVERE, "Failed to save config for ${player.uniqueId}", e)
        }
    }

    private fun reloadConfig() {
        config = loadConfig()
    }

    fun getPage(page: Int): MutableList<ItemStack> {
        reloadConfig()
        val section = config.getConfigurationSection("pages.page$page") ?: return mutableListOf()
        return section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()
    }

    fun addToPage(item: ItemStack, startPage: Int) {
        reloadConfig()
        var currentPage = startPage
        while (currentPage < 1000) {
            val sectionPath = "pages.page$currentPage"
            val section = config.getConfigurationSection(sectionPath) ?: config.createSection(sectionPath)
            val items = section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()

            if (items.size < 45) {
                section.set(items.size.toString(), item)
                saveConfig()
                return
            }
            currentPage++
        }
        UserShop.plugin.logger.warning("No available page for ${player.uniqueId} starting from $startPage")
    }

    fun setItemInSlot(page: Int, slot: Int, item: ItemStack?) {
        if (slot !in 0..44) return

        reloadConfig()
        val sectionPath = "pages.page$page"
        val section = config.getConfigurationSection(sectionPath)
            ?: config.createSection(sectionPath)
        section.set(slot.toString(), item)
        if (item==null) section.set(slot.toString(), null)
        saveConfig()
    }

    fun addEarnings(amount: Int) {
        reloadConfig()
        val currentEarnings = getEarnings()
        config.set("board.earnings", currentEarnings + amount)
        saveConfig()
    }

    fun getEarnings(): Int {
        reloadConfig()
        return config.getInt("board.earnings", 0)
    }

    fun clearEarnings() {
        reloadConfig()
        config.set("board.earnings", 0)
        saveConfig()
    }
}