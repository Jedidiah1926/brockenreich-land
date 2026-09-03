package com.brockenreich.landplugin.guild

import com.brockenreich.landplugin.util.parseUuidOrNull
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

class GuildManager(private val dataFolder: File, private val logger: Logger) {

    private val file = File(dataFolder, "guilds.yml")
    private val guilds: MutableMap<String, Guild> = mutableMapOf()

    fun guild(name: String): Guild? = guilds[name.lowercase()]

    fun guilds(): Collection<Guild> = guilds.values

    fun createGuild(name: String, leader: UUID): Guild {
        val key = name.lowercase()
        require(!guilds.containsKey(key)) { "이미 존재하는 길드 이름입니다: $name" }
        val guild = Guild(name, leader)
        guilds[key] = guild
        save()
        return guild
    }

    fun deleteGuild(name: String): Boolean {
        val removed = guilds.remove(name.lowercase()) != null
        if (removed) save()
        return removed
    }

    fun load() {
        guilds.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getConfigurationSection("guilds")?.getKeys(false)?.forEach { key ->
            val section = yaml.getConfigurationSection("guilds.$key") ?: return@forEach
            val leaderStr = section.getString("leader") ?: return@forEach
            val leader = parseUuidOrNull(leaderStr) ?: return@forEach
            val name = section.getString("name") ?: key
            val guild = Guild(name, leader)
            guild.officers.addAll(section.getStringList("officers").mapNotNull { parseUuidOrNull(it) })
            guild.members.addAll(section.getStringList("members").mapNotNull { parseUuidOrNull(it) })
            guilds[key] = guild
        }
        logger.info("Loaded ${guilds.size} guild(s).")
    }

    fun save() {
        val yaml = YamlConfiguration()
        guilds.forEach { (key, guild) ->
            val base = "guilds.$key"
            yaml.set("$base.name", guild.name)
            yaml.set("$base.leader", guild.leader.toString())
            yaml.set("$base.officers", guild.officers.map { it.toString() })
            yaml.set("$base.members", guild.members.map { it.toString() })
        }
        dataFolder.mkdirs()
        yaml.save(file)
    }
}
