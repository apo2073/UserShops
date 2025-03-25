package kr.apo2073.userShop

import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.java.JavaPlugin


class UserShop : JavaPlugin() {
    companion object {
        lateinit var plugin:UserShop
            private set
        lateinit var economy: Economy
    }
    override fun onEnable() {
        plugin=this

        setupEcon(this)
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
}
