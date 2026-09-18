package com.brockenreich.landplugin.nickname

import com.brockenreich.landplugin.display.PlayerDisplayManager
import com.brockenreich.landplugin.util.offlinePlayer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class NicknameCommand(
    private val nicknameManager: NicknameManager,
    private val displayManager: PlayerDisplayManager,
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.size) {
            0 -> sendUsage(sender)
            1 -> if (args[0].equals("reset", ignoreCase = true)) handleSelfReset(sender) else handleSelfSet(sender, args[0])
            else -> handleAdmin(sender, args)
        }
        return true
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /nickname 사용법 ---")
        sender.sendMessage("§e/nickname <새 닉네임> §7- 공백 없이 한 단어, & 색상 코드 사용 가능, 2~16자")
        sender.sendMessage("§e/nickname reset §7- 원래 계정 이름으로 되돌리기")
        sender.sendMessage("§e/nickname <플레이어> reset §7- OP 전용, 다른 플레이어의 닉네임 초기화")
        sender.sendMessage("§e/nickname <플레이어> <새 닉네임> §7- OP 전용, 다른 플레이어의 닉네임 강제 설정")
    }

    /** Length/character validation shared by self-service and admin-set; null (and messaged) if invalid. */
    private fun validate(sender: CommandSender, raw: String): String? {
        if (raw.length < 2 || raw.length > 16) {
            sender.sendMessage("§c닉네임은 2~16자여야 합니다.")
            return null
        }
        return ChatColor.translateAlternateColorCodes('&', raw)
    }

    private fun handleSelfSet(sender: CommandSender, raw: String) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        val nickname = validate(sender, raw) ?: return
        if (nicknameManager.isTaken(nickname, sender.uniqueId)) {
            sender.sendMessage("§c이미 사용 중인 닉네임입니다.")
            return
        }
        nicknameManager.set(sender.uniqueId, nickname)
        displayManager.refresh(sender)
        sender.sendMessage("§a닉네임을 설정했습니다: $nickname")
    }

    private fun handleSelfReset(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (nicknameManager.reset(sender.uniqueId)) {
            displayManager.refresh(sender)
            sender.sendMessage("§a닉네임을 초기화했습니다.")
        } else {
            sender.sendMessage("§c설정된 닉네임이 없습니다.")
        }
    }

    private fun handleAdmin(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("brockenreichland.nickname.admin")) {
            sender.sendMessage("§c이 명령은 관리자만 사용할 수 있습니다.")
            return
        }
        val targetName = args[0]
        val target = offlinePlayer(targetName)

        if (args[1].equals("reset", ignoreCase = true)) {
            if (nicknameManager.reset(target.uniqueId)) {
                target.player?.let { displayManager.refresh(it) }
                sender.sendMessage("§a$targetName 님의 닉네임을 초기화했습니다.")
            } else {
                sender.sendMessage("§c설정된 닉네임이 없습니다: $targetName")
            }
            return
        }

        val nickname = validate(sender, args[1]) ?: return
        if (nicknameManager.isTaken(nickname, target.uniqueId)) {
            sender.sendMessage("§c이미 사용 중인 닉네임입니다.")
            return
        }
        nicknameManager.set(target.uniqueId, nickname)
        target.player?.let { displayManager.refresh(it) }
        sender.sendMessage("§a$targetName 님의 닉네임을 $nickname (으)로 설정했습니다.")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> (listOf("reset") + Bukkit.getOnlinePlayers().map { it.name })
                .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> listOf("reset").filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
