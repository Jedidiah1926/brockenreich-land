package com.brockenreich.landplugin.display

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/** Re-applies each player's honor+nickname tab-list/nametag display (see PlayerDisplayManager) on join. */
class PlayerDisplayListener(private val displayManager: PlayerDisplayManager) : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onJoin(event: PlayerJoinEvent) {
        displayManager.refresh(event.player)
    }
}
