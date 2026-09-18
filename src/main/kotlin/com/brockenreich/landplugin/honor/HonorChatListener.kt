package com.brockenreich.landplugin.honor

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

/** Prefixes chat messages with the sender's equipped honor/title (see HonorManager). Chat display only, for now. */
class HonorChatListener(private val honorManager: HonorManager) : Listener {

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        val honor = honorManager.equipped(event.player.uniqueId) ?: return
        event.format = "§7[${honor.display}§7]§f %1\$s§f: %2\$s"
    }
}
