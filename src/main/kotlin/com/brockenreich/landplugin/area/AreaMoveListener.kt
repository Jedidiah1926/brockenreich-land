package com.brockenreich.landplugin.area

import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleMoveEvent

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

    // PlayerMoveEvent does not fire while a player is riding a vehicle - a boat moves via
    // VehicleMoveEvent instead, with its passengers along for the ride, so entrance/exit is
    // checked separately here (using dedicated boatEntrance/boatExit permissions) for boats.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVehicleMove(event: VehicleMoveEvent) {
        if (event.vehicle !is Boat) return
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY &&
            from.blockZ == to.blockZ && from.world == to.world
        ) {
            return
        }

        val players = event.vehicle.passengers.filterIsInstance<Player>()
        if (players.isEmpty()) return
        if (players.any { it.hasPermission("brockenreichland.area.bypass") }) return

        val fromArea = areaManager.areaAt(from)
        val toArea = areaManager.areaAt(to)
        if (fromArea === toArea) return

        if (players.any { !fromArea.can(it, AreaPermission.BOAT_EXIT) }) {
            event.isCancelled = true
            return
        }

        if (players.any { !toArea.can(it, AreaPermission.BOAT_ENTRANCE) }) {
            event.isCancelled = true
        }
    }
}
