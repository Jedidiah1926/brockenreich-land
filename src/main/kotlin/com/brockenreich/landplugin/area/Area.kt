package com.brockenreich.landplugin.area

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import java.util.UUID

enum class AreaPermission(val label: String) {
    ENTRANCE("entrance"),
    EXIT("exit"),
    INTERACTION("interaction"),
    PICKUP_ITEM("pickupItem"),
    DROP_ITEM("dropItem"),
    BLOCK_BREAK("blockBreak"),
    BLOCK_PLACE("blockPlace"),
    BLOCK_IGNITING("blockIgniting"),
    HANGING_PLACE("hangingPlace"),
    HANGING_BREAK("hangingBreak"),
    PROJECTILE_LAUNCH("projectileLaunch"),
    ATTACK_ENTITY("attackEntity"),
    ATTACK_PLAYER("attackPlayer"),
    BUCKET_EMPTY("bucketEmpty"),
    BUCKET_FILL("bucketFill"),
    ADMINISTRATION("administration");

    companion object {
        fun parse(value: String): AreaPermission? =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
    }
}

enum class AreaProtection(val label: String) {
    PISTON("piston"),
    FLOOD("flood"),
    POTION("potion"),
    EXPLOSION("explosion");

    companion object {
        fun parse(value: String): AreaProtection? =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
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
 * covered by any region). Members always have full access. `permissions` is the
 * @everyone default (every AreaPermission except ADMINISTRATION is allowed for a
 * newly created area); `playerPermissions` grants individual players extra access
 * on top of that default (it never revokes what @everyone already allows).
 * ADMINISTRATION is a wildcard: holding it (via @everyone or a per-player grant)
 * satisfies every other permission check in this area too.
 */
class Area(val target: AreaTarget, var world: String) {
    var min: Location? = null
    var max: Location? = null
    val members: MutableSet<UUID> = mutableSetOf()
    val permissions: MutableSet<AreaPermission> =
        AreaPermission.entries.filterTo(mutableSetOf()) { it != AreaPermission.ADMINISTRATION }
    val playerPermissions: MutableMap<UUID, MutableSet<AreaPermission>> = mutableMapOf()
    /** Structural protections (e.g. PISTON) that stop machinery from crossing this area's boundary. Off by default. */
    val protections: MutableSet<AreaProtection> = mutableSetOf()

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

    private fun granted(player: OfflinePlayer, permission: AreaPermission): Boolean =
        permissions.contains(permission) || (playerPermissions[player.uniqueId]?.contains(permission) ?: false)

    fun can(player: OfflinePlayer, permission: AreaPermission): Boolean =
        isMember(player) ||
            granted(player, permission) ||
            (permission != AreaPermission.ADMINISTRATION && granted(player, AreaPermission.ADMINISTRATION))

    fun canEnter(player: OfflinePlayer): Boolean = can(player, AreaPermission.ENTRANCE)

    fun canExit(player: OfflinePlayer): Boolean = can(player, AreaPermission.EXIT)
}
