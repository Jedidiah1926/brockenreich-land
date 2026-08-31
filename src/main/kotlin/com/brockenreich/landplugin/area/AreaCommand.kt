package com.brockenreich.landplugin.area

import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.bukkit.WorldEditPlugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AreaCommand(private val areaManager: AreaManager) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "region" -> handleRegion(sender, args)
            "world" -> handleWorld(sender, args)
            "list" -> handleList(sender)
            "info" -> handleInfo(sender, args)
            "modify" -> handleModify(sender, args)
            else -> sendUsage(sender)
        }
        return true
    }

    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage("§e--- /area 사용법 ---")
        sender.sendMessage("§e/area region create <이름> §7- WorldEdit 선택 영역으로 region:<이름> 생성")
        sender.sendMessage("§e/area region delete <이름>")
        sender.sendMessage("§e/area world create <월드이름> §7- world:<월드이름> 등록 (기본: administration 제외 모든 권한 허용)")
        sender.sendMessage("§e/area world delete <월드이름> §7- world:<월드이름> 을 기본값으로 초기화")
        sender.sendMessage("§e/area list")
        sender.sendMessage("§e/area info <region:이름|world:월드이름>")
        sender.sendMessage("§e/area modify <target> member add <닉네임>")
        sender.sendMessage("§e/area modify <target> member remove <닉네임>")
        sender.sendMessage("§e/area modify <target> role permission @everyone <add|remove> <권한>")
        sender.sendMessage("§e/area modify <target> role permission <닉네임> <add|remove> <권한>")
        sender.sendMessage(
            "§7권한: ${AreaPermission.entries.joinToString(", ") { it.label }}"
        )
    }

    private fun fmt(loc: Location) = "${loc.blockX},${loc.blockY},${loc.blockZ}"

    // ---- /area region ... ----

    private fun handleRegion(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /area region <create|delete> <이름>")
            return
        }
        when (args[1].lowercase()) {
            "create" -> handleRegionCreate(sender, args)
            "delete" -> handleRegionDelete(sender, args)
            else -> sender.sendMessage("§c'create' 또는 'delete' 이어야 합니다.")
        }
    }

    private fun handleRegionCreate(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return
        }
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /area region create <이름>")
            return
        }
        val name = args[2]
        if (areaManager.region(name) != null) {
            sender.sendMessage("§c이미 존재하는 구역 이름입니다: $name")
            return
        }

        val worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit") as? WorldEditPlugin
        if (worldEdit == null) {
            sender.sendMessage("§cWorldEdit 플러그인이 필요합니다.")
            return
        }

        val session = worldEdit.getSession(sender)
        val weWorld = BukkitAdapter.adapt(sender.world)
        val region = try {
            session.getSelection(weWorld)
        } catch (e: IncompleteRegionException) {
            sender.sendMessage("§cWorldEdit으로 먼저 영역을 선택해주세요 (나무도끼 또는 //pos1, //pos2).")
            return
        }

        val min = BukkitAdapter.adapt(sender.world, region.minimumPoint)
        val max = BukkitAdapter.adapt(sender.world, region.maximumPoint)

        val area = areaManager.createRegion(name, min, max)
        sender.sendMessage("§a구역 'region:$name' 을(를) 생성했습니다. (${area.world}: ${fmt(min)} ~ ${fmt(max)})")
    }

    private fun handleRegionDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /area region delete <이름>")
            return
        }
        val name = args[2]
        if (areaManager.deleteRegion(name)) {
            sender.sendMessage("§a구역 'region:$name' 을(를) 삭제했습니다.")
        } else {
            sender.sendMessage("§c존재하지 않는 구역입니다: region:$name")
        }
    }

    // ---- /area world ... ----

    private fun handleWorld(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /area world <create|delete> <월드이름>")
            return
        }
        when (args[1].lowercase()) {
            "create" -> handleWorldCreate(sender, args)
            "delete" -> handleWorldDelete(sender, args)
            else -> sender.sendMessage("§c'create' 또는 'delete' 이어야 합니다.")
        }
    }

    private fun handleWorldCreate(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /area world create <월드이름>")
            return
        }
        val name = args[2]
        if (Bukkit.getWorld(name) == null) {
            sender.sendMessage("§c존재하지 않는 월드입니다: $name")
            return
        }
        areaManager.worldArea(name)
        areaManager.save()
        sender.sendMessage("§a월드 구역 'world:$name' 을(를) 등록했습니다. (기본: administration 제외 모든 권한 허용)")
    }

    private fun handleWorldDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("§c사용법: /area world delete <월드이름>")
            return
        }
        val name = args[2]
        if (areaManager.resetWorldArea(name)) {
            sender.sendMessage("§a월드 구역 'world:$name' 을(를) 기본값(administration 제외 모든 권한 허용)으로 초기화했습니다.")
        } else {
            sender.sendMessage("§c등록되어 있지 않은 월드 구역입니다: world:$name")
        }
    }

    // ---- /area list / info ----

    private fun handleList(sender: CommandSender) {
        val regions = areaManager.regions()
        val worlds = areaManager.worldAreas()
        if (regions.isEmpty() && worlds.isEmpty()) {
            sender.sendMessage("§7등록된 구역이 없습니다.")
            return
        }
        sender.sendMessage("§e등록된 구역:")
        regions.forEach { sender.sendMessage("§7- ${it.target.key()}") }
        worlds.forEach { sender.sendMessage("§7- ${it.target.key()}") }
    }

    private fun handleInfo(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /area info <region:이름|world:월드이름>")
            return
        }
        val target = AreaTarget.parse(args[1])
        if (target == null) {
            sender.sendMessage("§c형식이 올바르지 않습니다. region:이름 또는 world:월드이름 형태여야 합니다.")
            return
        }
        val area = try {
            areaManager.area(target)
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c${e.message}")
            return
        }
        sender.sendMessage("§e구역 정보: ${target.key()}")
        sender.sendMessage("§7월드: ${area.world}")
        val min = area.min
        val max = area.max
        if (min != null && max != null) {
            sender.sendMessage("§7범위: ${fmt(min)} ~ ${fmt(max)}")
        }
        @Suppress("DEPRECATION")
        val memberNames = area.members.mapNotNull { Bukkit.getOfflinePlayer(it).name }
        sender.sendMessage("§7멤버: ${if (memberNames.isEmpty()) "없음" else memberNames.joinToString(", ")}")
        sender.sendMessage(
            "§7@everyone 허용 권한: ${if (area.permissions.isEmpty()) "없음" else area.permissions.joinToString(", ") { it.label }}"
        )
        if (area.playerPermissions.isNotEmpty()) {
            sender.sendMessage("§7개별 허용 권한:")
            area.playerPermissions.forEach { (uuid, perms) ->
                if (perms.isEmpty()) return@forEach
                @Suppress("DEPRECATION")
                val name = Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString()
                sender.sendMessage("§7 - $name: ${perms.joinToString(", ") { it.label }}")
            }
        }
    }

    // ---- /area modify ... ----

    private fun handleModify(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§c사용법: /area modify <target> ...")
            return
        }
        val target = AreaTarget.parse(args[1])
        if (target == null) {
            sender.sendMessage("§c형식이 올바르지 않습니다. region:이름 또는 world:월드이름 형태여야 합니다.")
            return
        }
        val area = try {
            areaManager.area(target)
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§c${e.message}")
            return
        }

        if (args.size < 3) {
            sender.sendMessage("§c사용법: /area modify ${args[1]} <member|role> ...")
            return
        }

        when (args[2].lowercase()) {
            "member" -> handleModifyMember(sender, area, args)
            "role" -> handleModifyRole(sender, area, args)
            else -> sender.sendMessage("§c'member' 또는 'role' 이어야 합니다.")
        }
    }

    private fun handleModifyMember(sender: CommandSender, area: Area, args: Array<out String>) {
        if (args.size < 5) {
            sender.sendMessage("§c사용법: /area modify ${args[1]} member <add|remove> <닉네임>")
            return
        }
        val action = args[3].lowercase()
        val nickname = args[4]
        @Suppress("DEPRECATION")
        val offlinePlayer = Bukkit.getOfflinePlayer(nickname)

        when (action) {
            "add" -> {
                area.members.add(offlinePlayer.uniqueId)
                sender.sendMessage("§a$nickname 을(를) ${area.target.key()} 의 멤버로 추가했습니다.")
            }
            "remove" -> {
                area.members.remove(offlinePlayer.uniqueId)
                sender.sendMessage("§a$nickname 을(를) ${area.target.key()} 의 멤버에서 제거했습니다.")
            }
            else -> {
                sender.sendMessage("§c'add' 또는 'remove' 이어야 합니다.")
                return
            }
        }
        areaManager.save()
    }

    private fun handleModifyRole(sender: CommandSender, area: Area, args: Array<out String>) {
        if (args.size < 7 || args[3].lowercase() != "permission") {
            sender.sendMessage("§c사용법: /area modify ${args[1]} role permission <@everyone|닉네임> <add|remove> <권한>")
            return
        }
        val subject = args[4]
        val action = args[5].lowercase()
        val permission = AreaPermission.parse(args[6])
        if (permission == null) {
            sender.sendMessage(
                "§c알 수 없는 권한입니다: ${args[6]} (사용 가능: ${AreaPermission.entries.joinToString(", ") { it.label }})"
            )
            return
        }
        if (action != "add" && action != "remove") {
            sender.sendMessage("§c'add' 또는 'remove' 이어야 합니다.")
            return
        }

        if (subject.startsWith("@")) {
            if (!subject.equals("@everyone", ignoreCase = true)) {
                sender.sendMessage("§c알 수 없는 역할입니다: $subject (사용 가능: @everyone)")
                return
            }
            when (action) {
                "add" -> area.permissions.add(permission)
                "remove" -> area.permissions.remove(permission)
            }
        } else {
            @Suppress("DEPRECATION")
            val offlinePlayer = Bukkit.getOfflinePlayer(subject)
            when (action) {
                "add" -> area.playerPermissions.getOrPut(offlinePlayer.uniqueId) { mutableSetOf() }.add(permission)
                "remove" -> area.playerPermissions[offlinePlayer.uniqueId]?.remove(permission)
            }
        }

        val verb = if (action == "add") "추가했습니다" else "제거했습니다"
        val subjectPhrase = if (subject.startsWith("@")) "§e[$subject]§f 역할에" else "§e[$subject]§f 님에게"
        sender.sendMessage(
            "§e[${area.target.key()}]§f 지역의 $subjectPhrase §e[${permission.label}]§f 권한을 $verb."
        )
        areaManager.save()
    }

    // ---- tab completion ----

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("region", "world", "list", "info", "modify").filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "region", "world" -> listOf("create", "delete").filter { it.startsWith(args[1].lowercase()) }
                "info", "modify" -> completeTargets(args[1])
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "region" -> if (args[1].lowercase() == "delete") {
                    areaManager.regions().map { it.target.key().removePrefix("region:") }
                        .filter { it.startsWith(args[2].lowercase()) }
                } else {
                    emptyList()
                }
                "world" -> if (args[1].lowercase() == "create" || args[1].lowercase() == "delete") {
                    Bukkit.getWorlds().map { it.name }.filter { it.startsWith(args[2].lowercase()) }
                } else {
                    emptyList()
                }
                "modify" -> listOf("member", "role").filter { it.startsWith(args[2].lowercase()) }
                else -> emptyList()
            }
            4 -> if (args[0].lowercase() == "modify") {
                when (args[2].lowercase()) {
                    "member" -> listOf("add", "remove").filter { it.startsWith(args[3].lowercase()) }
                    "role" -> listOf("permission").filter { it.startsWith(args[3].lowercase()) }
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
            5 -> if (args[0].lowercase() == "modify" && args[2].lowercase() == "role" && args[3].lowercase() == "permission") {
                (listOf("@everyone") + Bukkit.getOnlinePlayers().map { it.name })
                    .filter { it.startsWith(args[4], ignoreCase = true) }
            } else {
                emptyList()
            }
            6 -> if (args[0].lowercase() == "modify" && args[2].lowercase() == "role" && args[3].lowercase() == "permission") {
                listOf("add", "remove").filter { it.startsWith(args[5].lowercase()) }
            } else {
                emptyList()
            }
            7 -> if (args[0].lowercase() == "modify" && args[2].lowercase() == "role" && args[3].lowercase() == "permission") {
                AreaPermission.entries.map { it.label }.filter { it.startsWith(args[6], ignoreCase = true) }
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }

    private fun completeTargets(prefix: String): List<String> {
        val regionKeys = areaManager.regions().map { it.target.key() }
        val worldKeys = Bukkit.getWorlds().map { "world:${it.name}" }
        return (regionKeys + worldKeys).filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
