package com.brockenreich.landplugin.guild

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class GuildCommand(private val guildManager: GuildManager) : CommandExecutor, TabCompleter {

    private val subcommands = listOf("create", "disband", "invite", "kick", "promote", "demote", "leave", "info", "list")
    private val rosterActions = setOf("invite", "kick", "promote", "demote")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.getOrNull(0)?.lowercase() == "list") {
            handleList(sender)
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.")
            return true
        }
        when (args.getOrNull(0)?.lowercase()) {
            "create" -> handleCreate(sender, args)
            "disband" -> handleDisband(sender, args)
            "invite" -> handleInvite(sender, args)
            "kick" -> handleKick(sender, args)
            "promote" -> handlePromote(sender, args)
            "demote" -> handleDemote(sender, args)
            "leave" -> handleLeave(sender, args)
            "info" -> handleInfo(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /guild 사용법 ---")
        sender.sendMessage("§e/guild create <길드이름> §7- 누구나, 본인이 길드장이 됨")
        sender.sendMessage("§e/guild disband <길드이름> §7- 길드장 전용 (OP도 가능)")
        sender.sendMessage("§e/guild invite <길드이름> <닉네임> §7- 길드장/간부 전용")
        sender.sendMessage("§e/guild kick <길드이름> <닉네임> §7- 길드장/간부 전용")
        sender.sendMessage("§e/guild promote <길드이름> <닉네임> §7- 길드장 전용, 일반 길드원 → 간부")
        sender.sendMessage("§e/guild demote <길드이름> <닉네임> §7- 길드장 전용, 간부 → 일반 길드원")
        sender.sendMessage("§e/guild leave <길드이름> §7- 길드장은 탈퇴 불가 (해체해야 함)")
        sender.sendMessage("§e/guild info <길드이름>")
        sender.sendMessage("§e/guild list")
    }

    private fun handleCreate(sender: Player, args: Array<out String>) {
        val name = args.getOrNull(1)
        if (name == null) {
            sender.sendMessage("§c사용법: /guild create <길드이름>")
            return
        }
        if (guildManager.guild(name) != null) {
            sender.sendMessage("§c이미 존재하는 길드 이름입니다.")
            return
        }
        guildManager.createGuild(name, sender.uniqueId)
        sender.sendMessage("§a[$name] 길드를 만들었습니다. 당신이 길드장입니다.")
    }

    private fun handleDisband(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        if (guild.leader != sender.uniqueId && !sender.isOp) {
            sender.sendMessage("§c길드장만 길드를 해체할 수 있습니다.")
            return
        }
        guildManager.deleteGuild(guild.name)
        sender.sendMessage("§a[${guild.name}] 길드를 해체했습니다.")
    }

    private fun handleInvite(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        if (!guild.isOfficerOrAbove(sender.uniqueId)) {
            sender.sendMessage("§c길드장 또는 간부만 초대할 수 있습니다.")
            return
        }
        val nickname = args.getOrNull(2)
        if (nickname == null) {
            sender.sendMessage("§c사용법: /guild invite ${guild.name} <닉네임>")
            return
        }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(nickname)
        if (guild.isMember(target.uniqueId)) {
            sender.sendMessage("§c이미 길드원입니다.")
            return
        }
        guild.members.add(target.uniqueId)
        guildManager.save()
        sender.sendMessage("§a$nickname 을(를) [${guild.name}] 길드에 초대했습니다.")
    }

    private fun handleKick(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        if (!guild.isOfficerOrAbove(sender.uniqueId)) {
            sender.sendMessage("§c길드장 또는 간부만 추방할 수 있습니다.")
            return
        }
        val nickname = args.getOrNull(2)
        if (nickname == null) {
            sender.sendMessage("§c사용법: /guild kick ${guild.name} <닉네임>")
            return
        }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(nickname)
        if (target.uniqueId == guild.leader) {
            sender.sendMessage("§c길드장은 추방할 수 없습니다.")
            return
        }
        if (guild.officers.contains(target.uniqueId) && sender.uniqueId != guild.leader) {
            sender.sendMessage("§c간부는 길드장만 추방할 수 있습니다.")
            return
        }
        guild.officers.remove(target.uniqueId)
        guild.members.remove(target.uniqueId)
        guildManager.save()
        sender.sendMessage("§a$nickname 을(를) [${guild.name}] 길드에서 추방했습니다.")
    }

    private fun handlePromote(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        if (sender.uniqueId != guild.leader) {
            sender.sendMessage("§c길드장만 승급시킬 수 있습니다.")
            return
        }
        val nickname = args.getOrNull(2)
        if (nickname == null) {
            sender.sendMessage("§c사용법: /guild promote ${guild.name} <닉네임>")
            return
        }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(nickname)
        if (!guild.members.contains(target.uniqueId)) {
            sender.sendMessage("§c일반 길드원만 간부로 승급할 수 있습니다.")
            return
        }
        guild.members.remove(target.uniqueId)
        guild.officers.add(target.uniqueId)
        guildManager.save()
        sender.sendMessage("§a$nickname 을(를) 간부로 승급했습니다.")
    }

    private fun handleDemote(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        if (sender.uniqueId != guild.leader) {
            sender.sendMessage("§c길드장만 강등시킬 수 있습니다.")
            return
        }
        val nickname = args.getOrNull(2)
        if (nickname == null) {
            sender.sendMessage("§c사용법: /guild demote ${guild.name} <닉네임>")
            return
        }
        @Suppress("DEPRECATION")
        val target = Bukkit.getOfflinePlayer(nickname)
        if (!guild.officers.contains(target.uniqueId)) {
            sender.sendMessage("§c간부만 강등할 수 있습니다.")
            return
        }
        guild.officers.remove(target.uniqueId)
        guild.members.add(target.uniqueId)
        guildManager.save()
        sender.sendMessage("§a$nickname 을(를) 일반 길드원으로 강등했습니다.")
    }

    private fun handleLeave(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        if (sender.uniqueId == guild.leader) {
            sender.sendMessage("§c길드장은 탈퇴할 수 없습니다. 길드를 해체하세요 (/guild disband).")
            return
        }
        if (!guild.isMember(sender.uniqueId)) {
            sender.sendMessage("§c해당 길드의 길드원이 아닙니다.")
            return
        }
        guild.officers.remove(sender.uniqueId)
        guild.members.remove(sender.uniqueId)
        guildManager.save()
        sender.sendMessage("§a[${guild.name}] 길드에서 탈퇴했습니다.")
    }

    private fun handleInfo(sender: Player, args: Array<out String>) {
        val guild = requireGuild(sender, args.getOrNull(1)) ?: return
        @Suppress("DEPRECATION")
        val leaderName = Bukkit.getOfflinePlayer(guild.leader).name ?: guild.leader.toString()
        @Suppress("DEPRECATION")
        val officerNames = guild.officers.mapNotNull { Bukkit.getOfflinePlayer(it).name }
        @Suppress("DEPRECATION")
        val memberNames = guild.members.mapNotNull { Bukkit.getOfflinePlayer(it).name }

        sender.sendMessage("§e=== [${guild.name}] 길드 정보 ===")
        sender.sendMessage("§7길드장: $leaderName")
        sender.sendMessage("§7간부: ${if (officerNames.isEmpty()) "없음" else officerNames.joinToString(", ")}")
        sender.sendMessage("§7길드원: ${if (memberNames.isEmpty()) "없음" else memberNames.joinToString(", ")}")
    }

    private fun handleList(sender: CommandSender) {
        val names = guildManager.guilds().map { it.name }
        sender.sendMessage("§7길드 목록: ${if (names.isEmpty()) "없음" else names.joinToString(", ")}")
    }

    private fun requireGuild(sender: Player, name: String?): Guild? {
        val guild = name?.let { guildManager.guild(it) }
        if (guild == null) {
            sender.sendMessage("§c존재하지 않는 길드입니다.")
        }
        return guild
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> if (args[0].lowercase() in subcommands - "create" - "list") {
                guildManager.guilds().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            } else {
                emptyList()
            }
            3 -> if (args[0].lowercase() in rosterActions) {
                Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }
}
