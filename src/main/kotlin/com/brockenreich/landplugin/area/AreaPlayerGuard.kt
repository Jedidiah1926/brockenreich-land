package com.brockenreich.landplugin.area

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

/**
 * Safety net on top of the PlayerMoveEvent/PlayerTeleportEvent/PlayerBedEnterEvent boundary
 * checks in AreaMoveListener. Some position changes never go through those events at all - most
 * notably a piston physically shoving a player, which moves the entity directly rather than via
 * a movement packet the server can intercept and cancel. This instead runs every tick and
 * continuously verifies every online player (not currently riding a vehicle - AreaBoatGuard
 * already owns that case) is somewhere the entrance/exit permissions actually allow, snapping
 * them back to the last tick's confirmed-valid position otherwise, regardless of what caused the
 * move.
 */
class AreaPlayerGuard(private val plugin: Plugin, private val areaManager: AreaManager) {

    private val lastSafeLocation = mutableMapOf<UUID, Location>()

    fun start() {
        object : BukkitRunnable() {
            override fun run() = tick()
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun tick() {
        val seen = mutableSetOf<UUID>()

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.isInsideVehicle) continue
            seen.add(player.uniqueId)

            if (player.hasPermission("brockenreichland.area.bypass")) {
                lastSafeLocation[player.uniqueId] = player.location
                continue
            }

            val current = player.location
            val safe = lastSafeLocation[player.uniqueId]
            if (safe == null || safe.world != current.world) {
                // No known-good position yet (just joined/switched worlds) - accept this spot as
                // the baseline rather than fighting a position we have nothing to revert to.
                lastSafeLocation[player.uniqueId] = current
                continue
            }

            val fromArea = areaManager.areaAt(safe)
            val toArea = areaManager.areaAt(current)
            if (fromArea === toArea || (fromArea.canExit(player) && toArea.canEnter(player))) {
                lastSafeLocation[player.uniqueId] = current
            } else {
                player.teleport(safe)
            }
        }

        lastSafeLocation.keys.retainAll(seen)
    }
}
