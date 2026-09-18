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

    // Bukkit's Scoreboard.registerNewTeam(name) still hard-caps the team name at 16 characters
    // (even on modern versions - this isn't the old prefix/suffix limit, it's a separate one on
    // the name itself), so a per-player synthetic name like a UUID doesn't fit. The player's own
    // username is already guaranteed <=16 characters and globally unique, so it doubles as the
    // team name directly - no prefix needed.
    private fun teamFor(player: Player): Team {
        val board = Bukkit.getScoreboardManager()!!.mainScoreboard
        return board.getTeam(player.name) ?: board.registerNewTeam(player.name)
    }
}
