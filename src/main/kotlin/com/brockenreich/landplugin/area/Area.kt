package com.brockenreich.landplugin.area

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import java.util.UUID

enum class AreaPermission {
    ENTRANCE,
    EXIT;

    companion object {
        fun parse(value: String): AreaPermission? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

sealed class AreaTarget {
    data class WorldArea(val world: String) : AreaTarget()
    data class Region(val name: String) : AreaTarget()

    fun key(): String = when (this) {
        is WorldArea -> "world:$world"
        is Region -> "region:$name"
    }

    companion object {
        fun parse(value: String): AreaTarget? {
            val sep = value.indexOf(':')
            if (sep <= 0 || sep == value.length - 1) return null
            val prefix = value.substring(0, sep)
            val rest = value.substring(sep + 1)
            return when (prefix.lowercase()) {
                "world" -> WorldArea(rest)
                "region" -> Region(rest)
                else -> null
            }
        }
    }
}

/**
 * A named region (WorldEdit cuboid) or a world's catch-all area (everything not
 * covered by any region). Members always have entrance/exit access. `permissions`
 * is the @everyone default; `playerPermissions` grants individual players extra
 * access on top of that default (it never revokes what @everyone already allows).
 */
class Area(val target: AreaTarget, var world: String) {
    var min: Location? = null
    var max: Location? = null
    val members: MutableSet<UUID> = mutableSetOf()
    val permissions: MutableSet<AreaPermission> = mutableSetOf(AreaPermission.ENTRANCE, AreaPermission.EXIT)
    val playerPermissions: MutableMap<UUID, MutableSet<AreaPermission>> = mutableMapOf()

    fun contains(location: Location): Boolean {
        val min = this.min ?: return false
        val max = this.max ?: return false
        val locWorld = location.world?.name ?: return false
        if (locWorld != min.world?.name) return false
        // Compare block coordinates (both min/max are inclusive block corners from WorldEdit),
        // not raw continuous coordinates - otherwise the block spanning [max, max+1) is missed.
        return location.blockX >= min.blockX && location.blockX <= max.blockX &&
            location.blockY >= min.blockY && location.blockY <= max.blockY &&
            location.blockZ >= min.blockZ && location.blockZ <= max.blockZ
    }

    fun isMember(player: OfflinePlayer): Boolean = members.contains(player.uniqueId)

    private fun hasEffectivePermission(player: OfflinePlayer, permission: AreaPermission): Boolean =
        isMember(player) ||
            permissions.contains(permission) ||
            (playerPermissions[player.uniqueId]?.contains(permission) ?: false)

    fun canEnter(player: OfflinePlayer): Boolean = hasEffectivePermission(player, AreaPermission.ENTRANCE)

    fun canExit(player: OfflinePlayer): Boolean = hasEffectivePermission(player, AreaPermission.EXIT)
}
