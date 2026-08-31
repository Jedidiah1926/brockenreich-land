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
 * covered by any region). Members always have entrance/exit access; the
 * `permissions` set controls what non-members are allowed to do.
 */
class Area(val target: AreaTarget, var world: String) {
    var min: Location? = null
    var max: Location? = null
    val members: MutableSet<UUID> = mutableSetOf()
    val permissions: MutableSet<AreaPermission> = mutableSetOf(AreaPermission.ENTRANCE, AreaPermission.EXIT)

    fun contains(location: Location): Boolean {
        val min = this.min ?: return false
        val max = this.max ?: return false
        val locWorld = location.world?.name ?: return false
        if (locWorld != min.world?.name) return false
        return location.x >= min.x && location.x <= max.x &&
            location.y >= min.y && location.y <= max.y &&
            location.z >= min.z && location.z <= max.z
    }

    fun isMember(player: OfflinePlayer): Boolean = members.contains(player.uniqueId)

    fun canEnter(player: OfflinePlayer): Boolean =
        isMember(player) || permissions.contains(AreaPermission.ENTRANCE)

    fun canExit(player: OfflinePlayer): Boolean =
        isMember(player) || permissions.contains(AreaPermission.EXIT)
}
