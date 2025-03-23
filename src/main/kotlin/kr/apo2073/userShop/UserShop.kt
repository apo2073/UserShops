package kr.apo2073.userShop

import org.bukkit.plugin.java.JavaPlugin

class UserShop : JavaPlugin() {
    companion object {
        lateinit var plugin:UserShop
            private set
    }
    override fun onEnable() {
        plugin=this


    }
}
