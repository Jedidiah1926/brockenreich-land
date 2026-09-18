package com.brockenreich.landplugin.display

import com.brockenreich.landplugin.honor.HonorManager
import com.brockenreich.landplugin.nickname.NicknameManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

/**
 * Combines the equipped honor (HonorManager) and nickname (NicknameManager) into what a player's
 * tab-list entry shows, and applies the honor as a scoreboard-team prefix on their above-head
 * nametag.
 *
 * The above-head nametag is a Minecraft-level limitation, not a choice we made: Entity#setCustomName
 * does not actually override it for a *real player* entity (that only works for non-player
 * entities like mobs) - a scoreboard team's prefix/suffix, wrapped around the player's real
 * account name, is the only way a plugin can change what shows there without packet-level game
 * profile spoofing, which this plugin doesn't do (no ProtocolLib/NMS dependency). So the honor tag
 * shows above the head as a prefix, but the nickname itself can only replace the account name in
 * chat and the tab list - not in that one spot, where the real account name still shows through.
 *
 * Chat is separate from all of this: HonorChatListener prepends the honor tag to chat itself via
 * the event's format string, so `displayName` here is kept nickname-only - combining the honor
 * tag into it too would show it twice in chat.
 */
class PlayerDisplayManager(private val honorManager: HonorManager, private val nicknameManager: NicknameManager) {

    fun refresh(player: Player) {
        val nickname = nicknameManager.nickname(player.uniqueId) ?: player.name
        val honor = honorManager.equipped(player.uniqueId)
        val tabListName = if (honor != null) "§7[${honor.display}§7]§f $nickname" else nickname

        player.setDisplayName(nickname)
        player.setPlayerListName(tabListName)

        val team = teamFor(player)
        team.addEntry(player.name)
        team.prefix = if (honor != null) "§7[${honor.display}§7]§f " else ""
    }

    private fun teamFor(player: Player): Team {
        val board = Bukkit.getScoreboardManager()!!.mainScoreboard
        val name = TEAM_PREFIX + player.uniqueId
        return board.getTeam(name) ?: board.registerNewTeam(name)
    }

    companion object {
        private const val TEAM_PREFIX = "br_honor_"
    }
}
