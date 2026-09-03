package com.brockenreich.landplugin.economy

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EconomyCommand(private val economyManager: EconomyManager) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            handleBalance(sender, args)
            return true
        }
        when (args[0].lowercase()) {
            "balance", "bal" -> handleBalance(sender, args)
            "pay" -> handlePay(sender, args)
            "give" -> if (requireOp(sender)) handleGive(sender, args)
            "take" -> if (requireOp(sender)) handleTake(sender, args)
            "set" -> if (requireOp(sender)) handleSet(sender, args)
            "top" -> handleTop(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun requireOp(sender: CommandSender): Boolean {
        if (sender.hasPermission("brockenreichland.economy.admin")) return true
        sender.sendMessage("§c이 명령은 관리자만 사용할 수 있습니다.")
        return false
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /money 사용법 ---")
        sender.sendMessage("§e/money §7- 내 잔액 확인")
        sender.sendMessage("§e/money balance <플레이어> §7- 다른 플레이어 잔액 확인")
        sender.sendMessage("§e/money pay <플레이어> <금액> §7- 상대에게 송금")
        sender.sendMessage("§e/money give <플레이어> <금액> §7- OP 전용, 지급")
        sender.sendMessage("§e/money take <플레이어> <금액> §7- OP 전용, 회수")
        sender.sendMessage("§e/money set <플레이어> <금액> §7- OP 전용, 잔액 설정")
        sender.sendMessage("§e/money top [순위 개수] §7- 잔액 순위")
    }

    private fun parseAmount(sender: CommandSender, raw: String): Long? {
        val amount = raw.toLongOrNull()
        if (amount == null || amount <= 0) {
            sender.sendMessage("§c금액은 0보다 큰 정수여야 합니다: $raw")
            return null
        }
        return amount
    }

    private fun handleBalance(sender: CommandSender, args: Array<out String>) {
        val target = args.getOrNull(1)
        if (target == null) {
            if (sender !is Player) {
                sender.sendMessage("§c사용법: /money balance <플레이어>")
                return
            }
            sender.sendMessage("§e내 잔액: ${Currency.format(economyManager.balance(sender.uniqueId))}")
            return
        }
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(target)
        sender.sendMessage("§e$target 님의 잔액: ${Currency.format(economyManager.balance(offlinePlayer.uniqueId))}")
    }

    private fun handlePay(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /money pay <플레이어> <금액>")
            return
        }
        val targetName = args[1]
        val amount = parseAmount(sender, args[2]) ?: return
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)
        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage("§c자기 자신에게는 송금할 수 없습니다.")
            return
        }
        if (economyManager.transfer(sender.uniqueId, target.uniqueId, amount)) {
            val formatted = Currency.format(amount)
            sender.sendMessage("§a$targetName 님에게 $formatted 송금했습니다.")
            target.player?.sendMessage("§a${sender.name} 님에게서 $formatted 을(를) 받았습니다.")
        } else {
            sender.sendMessage("§c잔액이 부족합니다.")
        }
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /money give <플레이어> <금액>")
            return
        }
        val targetName = args[1]
        val amount = parseAmount(sender, args[2]) ?: return
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)
        economyManager.deposit(target.uniqueId, amount)
        sender.sendMessage("§a$targetName 님에게 ${Currency.format(amount)} 지급했습니다.")
    }

    private fun handleTake(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /money take <플레이어> <금액>")
            return
        }
        val targetName = args[1]
        val amount = parseAmount(sender, args[2]) ?: return
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)
        if (economyManager.withdraw(target.uniqueId, amount)) {
            sender.sendMessage("§a$targetName 님에게서 ${Currency.format(amount)} 회수했습니다.")
        } else {
            sender.sendMessage("§c대상의 잔액이 부족합니다.")
        }
    }

    private fun handleSet(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /money set <플레이어> <금액>")
            return
        }
        val targetName = args[1]
        val amount = args[2].toLongOrNull()
        if (amount == null || amount < 0) {
            sender.sendMessage("§c금액은 0 이상의 정수여야 합니다: ${args[2]}")
            return
        }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(targetName)
        economyManager.setBalance(target.uniqueId, amount)
        sender.sendMessage("§a$targetName 님의 잔액을 ${Currency.format(amount)} (으)로 설정했습니다.")
    }

    private fun handleTop(sender: CommandSender, args: Array<out String>) {
        val limit = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 50) ?: 10
        val top = economyManager.top(limit)
        if (top.isEmpty()) {
            sender.sendMessage("§7잔액 기록이 없습니다.")
            return
        }
        sender.sendMessage("§e--- 잔액 순위 (상위 $limit) ---")
        top.forEachIndexed { index, (uuid, amount) ->
            @Suppress("DEPRECATION")
            val name = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
            sender.sendMessage("§7${index + 1}. $name - ${Currency.format(amount)}")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("balance", "pay", "give", "take", "set", "top").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "balance", "pay", "give", "take", "set" ->
                    Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
