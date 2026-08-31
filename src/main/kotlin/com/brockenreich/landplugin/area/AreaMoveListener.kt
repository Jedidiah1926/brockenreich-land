package com.brockenreich.landplugin.area

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

class AreaMoveListener(private val areaManager: AreaManager) : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to ?: return
        if (from.blockX == to.blockX && from.blockY == to.blockY &&
            from.blockZ == to.blockZ && from.world == to.world
        ) {
            return
        }

        val player = event.player
        if (player.hasPermission("brockenreichland.area.bypass")) return

        val fromArea = areaManager.areaAt(from)
        val toArea = areaManager.areaAt(to)
        if (fromArea === toArea) return

        if (!fromArea.canExit(player)) {
            event.isCancelled = true
            return
        }

        if (!toArea.canEnter(player)) {
            event.isCancelled = true
        }
    }
}
