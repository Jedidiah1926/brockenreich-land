package com.brockenreich.landplugin.honor

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import net.md_5.bungee.api.ChatColor as BungeeChatColor

class HonorCommand(private val honorManager: HonorManager) : CommandExecutor, TabCompleter {

    // Just a hint palette for the HEX argument's tab-completion below - any valid #RRGGBB works,
    // these aren't validated against or otherwise special.
    private val hexPresets = listOf(
        "#FFFFFF", "#000000", "#FF5555", "#55FF55", "#5555FF",
        "#FFAA00", "#FF55FF", "#55FFFF", "#AA00AA", "#FFD700"
    )

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
        if (sender.hasPermission("brockenreichland.honor.admin")) return true
        sender.sendMessage("§c이 명령은 관리자만 사용할 수 있습니다.")
        return false
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /honor 사용법 ---")
        sender.sendMessage("§e/honor create <ID> <표시텍스트...> <HEX코드> §7- OP 전용, 표시텍스트에 & 색상 코드도 사용 가능")
        sender.sendMessage("§e/honor delete <ID> §7- OP 전용")
        sender.sendMessage("§e/honor grant <닉네임> <ID> §7- OP 전용")
        sender.sendMessage("§e/honor revoke <닉네임> <ID> §7- OP 전용")
        sender.sendMessage("§e/honor equip <ID> §7- 보유한 칭호를 채팅에 표시")
        sender.sendMessage("§e/honor unequip")
        sender.sendMessage("§e/honor mine §7- 내가 보유한 칭호 목록")
        sender.sendMessage("§e/honor list §7- 서버에 등록된 전체 칭호 목록")
        sender.sendMessage("§e/honor info <ID>")
    }

    private fun normalizeHex(input: String): String? {
        val cleaned = input.removePrefix("#")
        if (cleaned.length != 6) return null
        return if (cleaned.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) cleaned else null
    }

    private fun handleCreate(sender: CommandSender, args: Array<out String>) {
        if (args.size < 4) {
            sender.sendMessage("§c사용법: /honor create <ID> <표시텍스트...> <HEX코드>")
            return
        }
        val id = args[1]
        val hex = normalizeHex(args.last())
        if (hex == null) {
            sender.sendMessage("§cHEX 코드 형식이 올바르지 않습니다 (예: #FFAA00): ${args.last()}")
            return
        }
        val rawText = args.copyOfRange(2, args.size - 1).joinToString(" ")
        val color = BungeeChatColor.of("#$hex")
        val display = color.toString() + ChatColor.translateAlternateColorCodes('&', rawText)
        try {
            honorManager.createHonor(id, display)
            sender.sendMessage("§a칭호 '$id' 을(를) 생성했습니다: $display")
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c${e.message}")
        }
    }

    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /honor delete <ID>")
            return
        }
        val id = args[1]
        if (honorManager.deleteHonor(id)) {
            sender.sendMessage("§a칭호 '$id' 을(를) 삭제했습니다.")
        } else {
            sender.sendMessage("§c존재하지 않는 칭호입니다: $id")
        }
    }

    private fun handleGrant(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /honor grant <닉네임> <ID>")
            return
        }
        val nickname = args[1]
        val id = args[2]
        if (honorManager.honor(id) == null) {
            sender.sendMessage("§c존재하지 않는 칭호입니다: $id")
            return
        }
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(nickname)
        if (honorManager.grant(offlinePlayer.uniqueId, id)) {
            sender.sendMessage("§a$nickname 님에게 '$id' 칭호를 부여했습니다.")
        } else {
            sender.sendMessage("§c이미 보유하고 있는 칭호입니다.")
        }
    }

    private fun handleRevoke(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /honor revoke <닉네임> <ID>")
            return
        }
        val nickname = args[1]
        val id = args[2]
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(nickname)
        if (honorManager.revoke(offlinePlayer.uniqueId, id)) {
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
            sender.sendMessage("§c사용법: /honor equip <ID>")
            return
        }
        val id = args[1]
        if (honorManager.equip(sender.uniqueId, id)) {
            val display = honorManager.honor(id)?.display ?: id
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
        if (honorManager.unequip(sender.uniqueId)) {
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
        val owned = honorManager.grantedTo(sender.uniqueId)
        if (owned.isEmpty()) {
            sender.sendMessage("§7보유한 칭호가 없습니다.")
            return
        }
        val equippedId = honorManager.equipped(sender.uniqueId)?.id?.lowercase()
        sender.sendMessage("§e내가 보유한 칭호:")
        owned.forEach { id ->
            val honor = honorManager.honor(id)
            val display = honor?.display ?: id
            val marker = if (id == equippedId) " §a(장착 중)" else ""
            sender.sendMessage("§7- $id: $display$marker")
        }
    }

    private fun handleList(sender: CommandSender) {
        val honors = honorManager.honors()
        if (honors.isEmpty()) {
            sender.sendMessage("§7등록된 칭호가 없습니다.")
            return
        }
        sender.sendMessage("§e등록된 칭호:")
        honors.forEach { sender.sendMessage("§7- ${it.id}: ${it.display}") }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /honor info <ID>")
            return
        }
        val honor = honorManager.honor(args[1])
        if (honor == null) {
            sender.sendMessage("§c존재하지 않는 칭호입니다: ${args[1]}")
            return
        }
        sender.sendMessage("§e칭호 정보: ${honor.id}")
        sender.sendMessage("§7표시: ${honor.display}")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        // create's arg count is variable (<ID> <표시텍스트...> <HEX코드>), so from position 3 on we
        // can't tell whether the player is still typing display text or the trailing HEX code -
        // just hint the HEX palette at every such position rather than guessing.
        if (args.size >= 3 && args[0].equals("create", ignoreCase = true)) {
            return hexPresets.filter { it.startsWith(args.last(), ignoreCase = true) }
        }
        return when (args.size) {
            1 -> listOf("create", "delete", "grant", "revoke", "equip", "unequip", "mine", "list", "info")
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "delete", "info" ->
                    honorManager.honors().map { it.id }.filter { it.startsWith(args[1], ignoreCase = true) }
                "grant", "revoke" ->
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                "equip" -> if (sender is Player) {
                    honorManager.grantedTo(sender.uniqueId).mapNotNull { honorManager.honor(it)?.id }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                } else {
                    emptyList()
                }
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "grant", "revoke" ->
                    honorManager.honors().map { it.id }.filter { it.startsWith(args[2], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
