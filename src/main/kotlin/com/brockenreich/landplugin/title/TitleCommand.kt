package com.brockenreich.landplugin.title

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TitleCommand(private val titleManager: TitleManager) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }
        when (args[0].lowercase()) {
            "create" -> if (requireOp(sender)) handleCreate(sender, args)
            "delete" -> if (requireOp(sender)) handleDelete(sender, args)
            "grant" -> if (requireOp(sender)) handleGrant(sender, args)
            "revoke" -> if (requireOp(sender)) handleRevoke(sender, args)
            "equip" -> handleEquip(sender, args)
            "unequip" -> handleUnequip(sender)
            "mine" -> handleMine(sender)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun requireOp(sender: CommandSender): Boolean {
        if (sender.hasPermission("brockenreichland.title.admin")) return true
        sender.sendMessage("§c이 명령은 관리자만 사용할 수 있습니다.")
        return false
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /title 사용법 ---")
        sender.sendMessage("§e/title create <ID> <표시텍스트...> §7- OP 전용, & 로 색상 코드 사용 가능")
        sender.sendMessage("§e/title delete <ID> §7- OP 전용")
        sender.sendMessage("§e/title grant <닉네임> <ID> §7- OP 전용")
        sender.sendMessage("§e/title revoke <닉네임> <ID> §7- OP 전용")
        sender.sendMessage("§e/title equip <ID> §7- 보유한 칭호를 채팅에 표시")
        sender.sendMessage("§e/title unequip")
        sender.sendMessage("§e/title mine §7- 내가 보유한 칭호 목록")
        sender.sendMessage("§e/title list §7- 서버에 등록된 전체 칭호 목록")
        sender.sendMessage("§e/title info <ID>")
    }

    private fun handleCreate(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /title create <ID> <표시텍스트...>")
            return
        }
        val id = args[1]
        val display = ChatColor.translateAlternateColorCodes('&', args.drop(2).joinToString(" "))
        try {
            titleManager.createTitle(id, display)
            sender.sendMessage("§a칭호 '$id' 을(를) 생성했습니다: $display")
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c${e.message}")
        }
    }

    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /title delete <ID>")
            return
        }
        val id = args[1]
        if (titleManager.deleteTitle(id)) {
            sender.sendMessage("§a칭호 '$id' 을(를) 삭제했습니다.")
        } else {
            sender.sendMessage("§c존재하지 않는 칭호입니다: $id")
        }
    }

    private fun handleGrant(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /title grant <닉네임> <ID>")
            return
        }
        val nickname = args[1]
        val id = args[2]
        if (titleManager.title(id) == null) {
            sender.sendMessage("§c존재하지 않는 칭호입니다: $id")
            return
        }
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(nickname)
        if (titleManager.grant(offlinePlayer.uniqueId, id)) {
            sender.sendMessage("§a$nickname 님에게 '$id' 칭호를 부여했습니다.")
        } else {
            sender.sendMessage("§c이미 보유하고 있는 칭호입니다.")
        }
    }

    private fun handleRevoke(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /title revoke <닉네임> <ID>")
            return
        }
        val nickname = args[1]
        val id = args[2]
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(nickname)
        if (titleManager.revoke(offlinePlayer.uniqueId, id)) {
            sender.sendMessage("§a$nickname 님에게서 '$id' 칭호를 회수했습니다.")
        } else {
            sender.sendMessage("§c보유하고 있지 않은 칭호입니다.")
        }
    }

    private fun handleEquip(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /title equip <ID>")
            return
        }
        val id = args[1]
        if (titleManager.equip(sender.uniqueId, id)) {
            val display = titleManager.title(id)?.display ?: id
            sender.sendMessage("§a칭호를 장착했습니다: $display")
        } else {
            sender.sendMessage("§c보유하고 있지 않은 칭호입니다: $id")
        }
    }

    private fun handleUnequip(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (titleManager.unequip(sender.uniqueId)) {
            sender.sendMessage("§a칭호를 해제했습니다.")
        } else {
            sender.sendMessage("§c현재 장착 중인 칭호가 없습니다.")
        }
    }

    private fun handleMine(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        val owned = titleManager.grantedTo(sender.uniqueId)
        if (owned.isEmpty()) {
            sender.sendMessage("§7보유한 칭호가 없습니다.")
            return
        }
        val equippedId = titleManager.equipped(sender.uniqueId)?.id?.lowercase()
        sender.sendMessage("§e내가 보유한 칭호:")
        owned.forEach { id ->
            val title = titleManager.title(id)
            val display = title?.display ?: id
            val marker = if (id == equippedId) " §a(장착 중)" else ""
            sender.sendMessage("§7- $id: $display$marker")
        }
    }

    private fun handleList(sender: CommandSender) {
        val titles = titleManager.titles()
        if (titles.isEmpty()) {
            sender.sendMessage("§7등록된 칭호가 없습니다.")
            return
        }
        sender.sendMessage("§e등록된 칭호:")
        titles.forEach { sender.sendMessage("§7- ${it.id}: ${it.display}") }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /title info <ID>")
            return
        }
        val title = titleManager.title(args[1])
        if (title == null) {
            sender.sendMessage("§c존재하지 않는 칭호입니다: ${args[1]}")
            return
        }
        sender.sendMessage("§e칭호 정보: ${title.id}")
        sender.sendMessage("§7표시: ${title.display}")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("create", "delete", "grant", "revoke", "equip", "unequip", "mine", "list", "info")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "delete", "info" ->
                    titleManager.titles().map { it.id }.filter { it.startsWith(args[1], ignoreCase = true) }
                "grant", "revoke" ->
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                "equip" -> if (sender is Player) {
                    titleManager.grantedTo(sender.uniqueId).mapNotNull { titleManager.title(it)?.id }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "grant", "revoke" ->
                    titleManager.titles().map { it.id }.filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
