package com.brockenreich.landplugin

import org.bukkit.plugin.java.JavaPlugin

class LandPlugin : JavaPlugin() {

    override fun onEnable() {
        logger.info("BrockenreichLand enabled.")
    }

    override fun onDisable() {
        logger.info("BrockenreichLand disabled.")
    }
}
