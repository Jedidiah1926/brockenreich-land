package com.brockenreich.landplugin.area

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Boat
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.UUID

/**
 * Safety net on top of the VehicleMoveEvent-based boat boundary check in AreaMoveListener.
 * Reacting to a single move event can be outpaced by continuous paddling input (velocity keeps
 * getting re-applied faster than one teleport-back can undo it), so this instead runs every tick
 * and continuously verifies every boat currently carrying a player is sitting somewhere its
 * passengers are actually allowed (boatEntrance, membership, or bypass) - snapping it back to the
 * last tick's confirmed-valid position otherwise, regardless of what any single event saw.
 */
class AreaBoatGuard(private val plugin: Plugin, private val areaManager: AreaManager) {

    private val lastSafeLocation = mutableMapOf<UUID, Location>()

    fun start() {
        object : BukkitRunnable() {
            override fun run() = tick()
        }.runTaskTimer(plugin, 1L, 1L)
    }

    private fun tick() {
        val seen = mutableSetOf<UUID>()

        for (world in Bukkit.getWorlds()) {
            for (boat in world.entities.filterIsInstance<Boat>()) {
                val players = boat.passengers.filterIsInstance<Player>()
                if (players.isEmpty()) {
                    lastSafeLocation.remove(boat.uniqueId)
                    continue
                }
                seen.add(boat.uniqueId)

                if (players.any { it.hasPermission("brockenreichland.area.bypass") }) {
                    lastSafeLocation[boat.uniqueId] = boat.location
                    continue
                }

                val area = areaManager.areaAt(boat.location)
                val allowedHere = players.all { area.can(it, AreaPermission.BOAT_ENTRANCE) }

                if (allowedHere) {
                    lastSafeLocation[boat.uniqueId] = boat.location
                    continue
                }

                val safe = lastSafeLocation[boat.uniqueId]
                if (safe != null && safe.world == boat.world) {
                    boat.velocity = Vector(0.0, 0.0, 0.0)
                    boat.teleport(safe)
                } else {
                    // No known-good position yet (e.g. spawned/placed directly here) - accept this
                    // spot as the baseline rather than fighting a position we have nothing to
                    // revert to.
                    lastSafeLocation[boat.uniqueId] = boat.location
                }
            }
        }

        lastSafeLocation.keys.retainAll(seen)
    }
}
