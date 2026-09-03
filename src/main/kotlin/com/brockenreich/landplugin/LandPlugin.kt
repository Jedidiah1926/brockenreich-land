package com.brockenreich.landplugin

import com.brockenreich.landplugin.area.AreaBoatGuard
import com.brockenreich.landplugin.area.AreaCommand
import com.brockenreich.landplugin.area.AreaManager
import com.brockenreich.landplugin.area.AreaMoveListener
import com.brockenreich.landplugin.area.AreaPlayerGuard
import com.brockenreich.landplugin.area.AreaProtectionListener
import com.brockenreich.landplugin.economy.EconomyCommand
import com.brockenreich.landplugin.economy.EconomyManager
import com.brockenreich.landplugin.farm.FarmCommand
import com.brockenreich.landplugin.farm.FarmItems
import com.brockenreich.landplugin.farm.FarmListener
import com.brockenreich.landplugin.farm.FarmManager
import com.brockenreich.landplugin.guild.GuildCommand
import com.brockenreich.landplugin.guild.GuildManager
import com.brockenreich.landplugin.honor.HonorChatListener
import com.brockenreich.landplugin.honor.HonorCommand
import com.brockenreich.landplugin.honor.HonorManager
import com.brockenreich.landplugin.nickname.NicknameCommand
import com.brockenreich.landplugin.nickname.NicknameListener
import com.brockenreich.landplugin.nickname.NicknameManager
import org.bukkit.plugin.java.JavaPlugin

class LandPlugin : JavaPlugin() {

    lateinit var areaManager: AreaManager
        private set

    lateinit var farmManager: FarmManager
        private set

    lateinit var guildManager: GuildManager
        private set

    lateinit var honorManager: HonorManager
        private set

    lateinit var nicknameManager: NicknameManager
        private set

    lateinit var economyManager: EconomyManager
        private set

    override fun onEnable() {
        guildManager = GuildManager(dataFolder, logger)
        guildManager.load()

        getCommand("guild")?.let { command ->
            val executor = GuildCommand(guildManager)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }

        areaManager = AreaManager(dataFolder, logger, guildManager)
        areaManager.load()

        getCommand("area")?.let { command ->
            val executor = AreaCommand(areaManager, guildManager)
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
            val executor = FarmCommand(farmManager, farmItems)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }
        server.pluginManager.registerEvents(FarmListener(farmManager, farmItems), this)

        honorManager = HonorManager(dataFolder, logger)
        honorManager.load()

        getCommand("honor")?.let { command ->
            val executor = HonorCommand(honorManager)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }
        server.pluginManager.registerEvents(HonorChatListener(honorManager), this)

        nicknameManager = NicknameManager(dataFolder, logger)
        nicknameManager.load()

        getCommand("nickname")?.let { command ->
            val executor = NicknameCommand(nicknameManager)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }
        server.pluginManager.registerEvents(NicknameListener(nicknameManager), this)

        economyManager = EconomyManager(dataFolder, logger)
        economyManager.load()

        getCommand("money")?.let { command ->
            val executor = EconomyCommand(economyManager)
            command.setExecutor(executor)
            command.tabCompleter = executor
        }

        logger.info("BrockenreichLand enabled.")
    }

    override fun onDisable() {
        if (::areaManager.isInitialized) {
            areaManager.save()
        }
        if (::farmManager.isInitialized) {
            farmManager.save()
        }
        if (::guildManager.isInitialized) {
            guildManager.save()
        }
        if (::honorManager.isInitialized) {
            honorManager.save()
        }
        if (::nicknameManager.isInitialized) {
            nicknameManager.save()
        }
        if (::economyManager.isInitialized) {
            economyManager.save()
        }
        logger.info("BrockenreichLand disabled.")
    }
}
