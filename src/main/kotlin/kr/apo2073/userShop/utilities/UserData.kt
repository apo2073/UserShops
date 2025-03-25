package kr.apo2073.userShop.utilities

import kr.apo2073.userShop.UserShop
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.io.File

class UserData(private val player: Player) {
    private val file = File(UserShop.plugin.dataFolder, "userdata/${player.uniqueId}.yml").apply { parentFile.mkdirs() }
    private val config = YamlConfiguration.loadConfiguration(file)

    fun getPage(page: Int): MutableList<ItemStack> {
        val section = config.getConfigurationSection("shop.page.$page") ?: return mutableListOf()
        return section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()
    }

    fun addToPage(item: ItemStack, startPage: Int) {
        var currentPage = startPage
        while (currentPage < 1000) {
            val sectionPath = "shop.page.$currentPage"
            val section = config.getConfigurationSection(sectionPath) ?: config.createSection(sectionPath)
            val items = section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()

            if (items.size < 45) {
                val newKey = items.size.toString()
                section.set(newKey, item)
                config.save(file)
                return
            }
            currentPage++
        }
    }

    fun setItemInSlot(page: Int, slot: Int, item: ItemStack?) {
        if (slot !in 0..44) {
            throw IllegalArgumentException("Slot must be between 0 and 44")
        }

        val sectionPath = "shop.page.$page"
        val section = config.getConfigurationSection(sectionPath) ?: config.createSection(sectionPath)
        section.set(slot.toString(), item)
        config.save(file)
    }

    fun addEarnings(amount: Int) {
        val current = getEarnings()
        config.set("board.earnings", current + amount)
        config.save(file)
    }

    fun getEarnings(): Int {
        return config.getInt("board.earnings", 0)
    }

    fun clearEarnings() {
        config.set("board.earnings", 0)
        config.save(file)
    }
}