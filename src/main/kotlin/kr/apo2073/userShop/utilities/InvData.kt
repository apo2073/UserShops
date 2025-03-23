package kr.apo2073.userShop.utilities

import kr.apo2073.userShop.UserShop
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File

class InvData {
    fun getPage(page: Int): MutableList<ItemStack> {
        val file = File(UserShop.plugin.dataFolder, "invData/shop.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        val section = config.getConfigurationSection("shop.page.$page") ?: return mutableListOf()
        return section.getKeys(false).mapNotNull { section.getItemStack(it) }.toMutableList()
    }

    fun addToPage(item: ItemStack, startPage: Int) {
        val file = File(UserShop.plugin.dataFolder, "invData/shop.yml").apply { parentFile.mkdirs() }
        val config = YamlConfiguration.loadConfiguration(file)

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
}