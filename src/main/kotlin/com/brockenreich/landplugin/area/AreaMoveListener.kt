package com.brockenreich.landplugin.area

import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.util.Vector

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

    // Ender pearls and chorus fruit teleport the player directly, bypassing PlayerMoveEvent
    // entirely - without this, entrance/exit permissions could be skipped just by throwing a
    // pearl or eating chorus fruit across a boundary. PlayerTeleportEvent IS Cancellable (unlike
    // VehicleMoveEvent/BlockRedstoneEvent), so ignoreCancelled is safe here.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL &&
            event.cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
        ) {
            return
        }

        val from = event.from
        val to = event.to ?: return
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

    // Sleeping snaps the player's exact position onto the bed directly (not through a movement
    // packet), so PlayerMoveEvent never sees it - without this, a bed straddling a boundary would
    // let someone sleep their way into/out of an area with no entrance/exit permission.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBedEnter(event: PlayerBedEnterEvent) {
        val player = event.player
        if (player.hasPermission("brockenreichland.area.bypass")) return

        val fromArea = areaManager.areaAt(player.location)
        val toArea = areaManager.areaAt(event.bed.location)
        if (fromArea === toArea) return

        if (!fromArea.canExit(player) || !toArea.canEnter(player)) {
            event.isCancelled = true
        }
    }

    // PlayerMoveEvent does not fire while a player is riding a vehicle - a boat moves via
    // VehicleMoveEvent instead, with its passengers along for the ride, so entrance/exit is
    // checked separately here (using dedicated boatEntrance/boatExit permissions) for boats.
    // VehicleMoveEvent isn't Cancellable, so a denied move is undone by teleporting the boat
    // back to its previous location instead of setting isCancelled - and `ignoreCancelled` must
    // NOT be set here, since Bukkit's generated executor casts to Cancellable to honor it and
    // VehicleMoveEvent doesn't implement that interface, throwing at every single firing.
    @EventHandler(priority = EventPriority.LOW)
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

        val denied = players.any { !fromArea.can(it, AreaPermission.BOAT_EXIT) } ||
            players.any { !toArea.can(it, AreaPermission.BOAT_ENTRANCE) }
        if (denied) {
            // Zero the velocity too - paddling keeps pushing momentum into the boat each tick, and
            // a teleport alone doesn't clear that, letting it creep through over several ticks.
            event.vehicle.velocity = Vector(0.0, 0.0, 0.0)
            event.vehicle.teleport(from)
        }
    }

    // Closes a bypass: a boat parked straddling a boundary can sit there without ever completing
    // a block-crossing move (so onVehicleMove above never fires a denial), but dismounting still
    // plants the player on foot wherever the boat currently is. Only block the dismount itself
    // when the player has neither walking `entrance` nor `boatEntrance` for that spot - if
    // boatEntrance is what let them in, they must still be able to get out of the boat there.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onVehicleExit(event: VehicleExitEvent) {
        if (event.vehicle !is Boat) return
        val player = event.exited as? Player ?: return
        if (player.hasPermission("brockenreichland.area.bypass")) return

        val area = areaManager.areaAt(event.vehicle.location)
        if (!area.canEnter(player) && !area.can(player, AreaPermission.BOAT_ENTRANCE)) {
            event.isCancelled = true
        }
    }
}
