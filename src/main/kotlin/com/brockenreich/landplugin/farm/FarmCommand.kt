package com.brockenreich.landplugin.farm

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class FarmCommand(private val farmManager: FarmManager, private val farmItems: FarmItems) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "animate" -> handleAnimate(sender)
            "time" -> handleTime(sender, args)
            else -> sender.sendMessage("§c사용법: /farm animate 또는 /farm time <test [초]|default>")
        }
        return true
    }

    private fun handleAnimate(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.")
            return
        }

        val item = sender.inventory.itemInMainHand
        val type = FarmCropType.forItem(item.type)
        if (type == null) {
            sender.sendMessage("§c손에 들고 있는 아이템은 인챈트할 수 없습니다.")
            return
        }
        if (farmItems.isAnimated(item)) {
            sender.sendMessage("§c이미 인챈트된 아이템입니다.")
            return
        }

        farmItems.animate(item, type.displayName)
        sender.sendMessage("§a${type.displayName} 아이템을 인챈트했습니다.")
    }

    private fun handleTime(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("brockenreichland.farm.admin")) {
            sender.sendMessage("§c이 명령은 관리자만 사용할 수 있습니다.")
            return
        }
        when (args.getOrNull(1)?.lowercase()) {
            "test" -> {
                val requested = args.getOrNull(2)
                val seconds = requested?.toLongOrNull()
                if (requested != null && (seconds == null || seconds <= 0)) {
                    sender.sendMessage("§c초는 1 이상의 정수로 입력해주세요.")
                    return
                }
                val actual = seconds ?: 10L
                farmManager.setTestSeconds(actual)
                sender.sendMessage("§a테스트 모드: 지금부터 심는 모든 작물의 성장 시간이 ${actual}초로 적용됩니다.")
            }
            "default" -> {
                farmManager.setTestSeconds(null)
                sender.sendMessage("§a설정된 기본 성장 시간으로 되돌렸습니다.")
            }
            else -> sender.sendMessage("§c사용법: /farm time <test [초]|default>")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("animate", "time").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].equals("time", ignoreCase = true)) {
                listOf("test", "default").filter { it.startsWith(args[1], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }
}
