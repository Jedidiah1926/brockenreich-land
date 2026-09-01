package com.brockenreich.landplugin.farm

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class FarmCommand(private val farmItems: FarmItems) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.")
            return true
        }
        if (args.isEmpty() || !args[0].equals("animate", ignoreCase = true)) {
            sender.sendMessage("§c사용법: /farm animate")
            return true
        }

        val item = sender.inventory.itemInMainHand
        val type = FarmCropType.forItem(item.type)
        if (type == null) {
            sender.sendMessage("§c손에 들고 있는 아이템은 인챈트할 수 없습니다.")
            return true
        }
        if (farmItems.isAnimated(item)) {
            sender.sendMessage("§c이미 인챈트된 아이템입니다.")
            return true
        }

        farmItems.animate(item, type.displayName)
        sender.sendMessage("§a${type.displayName} 아이템을 인챈트했습니다.")
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("animate").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}
