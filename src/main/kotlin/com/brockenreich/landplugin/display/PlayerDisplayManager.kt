package com.brockenreich.landplugin.display

import com.brockenreich.landplugin.honor.HonorManager
import com.brockenreich.landplugin.nickname.NicknameManager
import org.bukkit.entity.Player

/**
 * Combines the equipped honor (HonorManager) and nickname (NicknameManager) into what a player's
 * tab-list entry and above-head nametag show - the one place both systems need to agree, since
 * either one changing (equip/unequip an honor, set/reset a nickname) needs to redraw both.
 *
 * Chat is separate: HonorChatListener prepends the honor tag to chat itself via the event's
 * format string, so `displayName` here is kept nickname-only - combining the honor tag into it
 * too would show it twice in chat.
 */
class PlayerDisplayManager(private val honorManager: HonorManager, private val nicknameManager: NicknameManager) {

    fun refresh(player: Player) {
        val nickname = nicknameManager.nickname(player.uniqueId) ?: player.name
        val honor = honorManager.equipped(player.uniqueId)
        val display = if (honor != null) "§7[${honor.display}§7]§f $nickname" else nickname

        player.setDisplayName(nickname)
        player.setPlayerListName(display)
        player.setCustomName(display)
        player.isCustomNameVisible = true
    }
}
