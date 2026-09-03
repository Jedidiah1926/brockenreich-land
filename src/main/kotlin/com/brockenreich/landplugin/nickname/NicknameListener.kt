package com.brockenreich.landplugin.nickname

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/** Applies each player's saved nickname (see NicknameManager) to their chat/tab-list display name on join. */
class NicknameListener(private val nicknameManager: NicknameManager) : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onJoin(event: PlayerJoinEvent) {
        val nickname = nicknameManager.nickname(event.player.uniqueId) ?: return
        apply(event.player, nickname)
    }

    companion object {
        /** Sets (or clears, when [nickname] is null) a player's chat/tab-list display name. */
        fun apply(player: Player, nickname: String?) {
            player.setDisplayName(nickname ?: player.name)
            player.setPlayerListName(nickname ?: player.name)
        }
    }
}
