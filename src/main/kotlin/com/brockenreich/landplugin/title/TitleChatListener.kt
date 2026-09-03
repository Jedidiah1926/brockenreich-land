package com.brockenreich.landplugin.title

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

/** Prefixes chat messages with the sender's equipped title (see TitleManager). Chat display only, for now. */
class TitleChatListener(private val titleManager: TitleManager) : Listener {

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val title = titleManager.equipped(event.player.uniqueId) ?: return
        event.format = "§7[${title.display}§7]§f %1\$s§f: %2\$s"
    }
}
