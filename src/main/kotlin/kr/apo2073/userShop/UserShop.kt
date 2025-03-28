package kr.apo2073.userShop

import kr.apo2073.userShop.cmds.UserCmd
import kr.apo2073.userShop.inv.AddItemHolder
import kr.apo2073.userShop.inv.ShopHolder
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin


class UserShop : JavaPlugin() {
    companion object {
        lateinit var plugin:UserShop
            private set
        lateinit var economy: Economy
    }
    override fun onEnable() {
        plugin=this

        saveDefaultConfig()
        setupEcon(this)
        server.pluginManager.registerEvents(AddItemHolder(), this)
        server.pluginManager.registerEvents(ShopHolder(), this)
        getCommand("게시판")?.apply {
            setExecutor(UserCmd())
            tabCompleter=UserCmd()
        }
    }

    private fun setupEcon(plugin: JavaPlugin): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            plugin.server.pluginManager.disablePlugin(plugin)
        }
        val rsp = plugin.server.servicesManager.getRegistration(
            Economy::class.java
        )
        if (rsp == null) return false
        economy = rsp.provider
        return economy != null
    }

    fun updateInventory() {
        Bukkit.getScheduler().runTask(this, Runnable {
            Bukkit.getOnlinePlayers().forEach { it.updateInventory() }
        })
    }
}
