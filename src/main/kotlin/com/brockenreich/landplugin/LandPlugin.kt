package com.brockenreich.landplugin

import com.brockenreich.landplugin.area.AreaBoatGuard
import com.brockenreich.landplugin.area.AreaCommand
import com.brockenreich.landplugin.area.AreaManager
import com.brockenreich.landplugin.area.AreaMoveListener
import com.brockenreich.landplugin.area.AreaPlayerGuard
import com.brockenreich.landplugin.area.AreaProtectionListener
import com.brockenreich.landplugin.farm.FarmCommand
import com.brockenreich.landplugin.farm.FarmItems
import com.brockenreich.landplugin.farm.FarmListener
import com.brockenreich.landplugin.farm.FarmManager
import org.bukkit.plugin.java.JavaPlugin

class LandPlugin : JavaPlugin() {

    lateinit var areaManager: AreaManager
        private set

    lateinit var farmManager: FarmManager
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
        server.pluginManager.registerEvents(AreaProtectionListener(areaManager), this)
        AreaBoatGuard(this, areaManager).start()
        AreaPlayerGuard(this, areaManager).start()

        saveDefaultConfig()
        farmManager = FarmManager(this, dataFolder)
        farmManager.loadConfig(config)
        farmManager.load()
        farmManager.start()

        val farmItems = FarmItems(this)
        getCommand("farm")?.let { command ->
            val executor = FarmCommand(farmItems)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }
        server.pluginManager.registerEvents(FarmListener(farmManager, farmItems), this)

        logger.info("BrockenreichLand enabled.")
    }

    override fun onDisable() {
        if (::areaManager.isInitialized) {
            areaManager.save()
        }
        if (::farmManager.isInitialized) {
            farmManager.save()
        }
        logger.info("BrockenreichLand disabled.")
    }
}
