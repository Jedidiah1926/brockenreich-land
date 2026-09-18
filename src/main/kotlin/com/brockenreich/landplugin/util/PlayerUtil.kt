package com.brockenreich.landplugin.util

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.UUID

/** Resolves a player by name, trusting the input without an online/seen-before check - this
 *  plugin's commands look players up this way throughout (e.g. to grant a role to someone
 *  currently offline). */
@Suppress("DEPRECATION")
fun offlinePlayer(name: String): OfflinePlayer = Bukkit.getOfflinePlayer(name)

/** Parses [value] as a UUID, or null if it isn't one - used when loading YAML keys expected to be UUIDs. */
fun parseUuidOrNull(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()
