package com.brockenreich.landplugin

import com.brockenreich.landplugin.area.AreaCommand
import com.brockenreich.landplugin.area.AreaManager
import com.brockenreich.landplugin.area.AreaMoveListener
import org.bukkit.plugin.java.JavaPlugin

class LandPlugin : JavaPlugin() {

    lateinit var areaManager: AreaManager
        private set

    override fun onEnable() {
        areaManager = AreaManager(dataFolder, logger)
        areaManager.load()

        getCommand("area")?.let { command ->
            val executor = AreaCommand(areaManager)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }

        server.pluginManager.registerEvents(AreaMoveListener(areaManager), this)

        logger.info("BrockenreichLand enabled.")
    }

    override fun onDisable() {
        if (::areaManager.isInitialized) {
            areaManager.save()
        }
        logger.info("BrockenreichLand disabled.")
    }
}
