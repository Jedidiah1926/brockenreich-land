package com.brockenreich.landplugin.nickname

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class NicknameCommand(private val nicknameManager: NicknameManager) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }
        if (args[0].equals("reset", ignoreCase = true)) {
            handleReset(sender, args)
        } else {
            handleSet(sender, args)
        }
        return true
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /nickname 사용법 ---")
        sender.sendMessage("§e/nickname <새 닉네임> §7- 공백 없이 한 단어, & 색상 코드 사용 가능, 2~16자")
        sender.sendMessage("§e/nickname reset §7- 원래 계정 이름으로 되돌리기")
        sender.sendMessage("§e/nickname reset <플레이어> §7- OP 전용, 다른 플레이어의 닉네임 초기화")
    }

    private fun handleSet(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (args.size != 1) {
            sender.sendMessage("§c사용법: /nickname <새 닉네임> §7(공백 없이 한 단어)")
            return
        }
        val raw = args[0]
        if (raw.length < 2 || raw.length > 16) {
            sender.sendMessage("§c닉네임은 2~16자여야 합니다.")
            return
        }
        val nickname = ChatColor.translateAlternateColorCodes('&', raw)
        if (nicknameManager.isTaken(nickname, sender.uniqueId)) {
            sender.sendMessage("§c이미 사용 중인 닉네임입니다.")
            return
        }
        nicknameManager.set(sender.uniqueId, nickname)
        NicknameListener.apply(sender, nickname)
        sender.sendMessage("§a닉네임을 설정했습니다: $nickname")
    }

    private fun handleReset(sender: CommandSender, args: Array<out String>) {
        if (args.size >= 2) {
            if (!sender.hasPermission("brockenreichland.nickname.admin")) {
                sender.sendMessage("§c이 명령은 관리자만 사용할 수 있습니다.")
                return
            }
            val nickname = args[1]
            @Suppress("DEPRECATION")
            val offlinePlayer = Bukkit.getOfflinePlayer(nickname)
            if (nicknameManager.reset(offlinePlayer.uniqueId)) {
                offlinePlayer.player?.let { NicknameListener.apply(it, null) }
                sender.sendMessage("§a$nickname 님의 닉네임을 초기화했습니다.")
            } else {
                sender.sendMessage("§c설정된 닉네임이 없습니다: $nickname")
            }
            return
        }
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (nicknameManager.reset(sender.uniqueId)) {
            NicknameListener.apply(sender, null)
            sender.sendMessage("§a닉네임을 초기화했습니다.")
        } else {
            sender.sendMessage("§c설정된 닉네임이 없습니다.")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("reset").filter { it.startsWith(args[0].lowercase()) }
            2 -> if (args[0].equals("reset", ignoreCase = true)) {
                Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }
}
