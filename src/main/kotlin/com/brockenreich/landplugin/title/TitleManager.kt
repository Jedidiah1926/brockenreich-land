package com.brockenreich.landplugin.title

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

class TitleManager(private val dataFolder: File, private val logger: Logger) {

    private val file = File(dataFolder, "titles.yml")
    private val titles: MutableMap<String, Title> = mutableMapOf()
    private val granted: MutableMap<UUID, MutableSet<String>> = mutableMapOf()
    private val equipped: MutableMap<UUID, String> = mutableMapOf()

    fun title(id: String): Title? = titles[id.lowercase()]

    fun titles(): Collection<Title> = titles.values

    fun createTitle(id: String, display: String): Title {
        val key = id.lowercase()
        require(!titles.containsKey(key)) { "이미 존재하는 칭호 ID입니다: $id" }
        val title = Title(id, display)
        titles[key] = title
        save()
        return title
    }

    fun deleteTitle(id: String): Boolean {
        val key = id.lowercase()
        val removed = titles.remove(key) != null
        if (removed) {
            granted.values.forEach { it.remove(key) }
            equipped.entries.removeIf { it.value == key }
            save()
        }
        return removed
    }

    fun isGranted(uuid: UUID, id: String): Boolean = granted[uuid]?.contains(id.lowercase()) ?: false

    /** Title IDs (lowercase) [uuid] currently owns. */
    fun grantedTo(uuid: UUID): Set<String> = granted[uuid] ?: emptySet()

    fun grant(uuid: UUID, id: String): Boolean {
        val key = id.lowercase()
        if (!titles.containsKey(key)) return false
        val added = granted.getOrPut(uuid) { mutableSetOf() }.add(key)
        if (added) save()
        return added
    }

    fun revoke(uuid: UUID, id: String): Boolean {
        val key = id.lowercase()
        val removed = granted[uuid]?.remove(key) ?: false
        if (removed) {
            if (equipped[uuid] == key) equipped.remove(uuid)
            save()
        }
        return removed
    }

    fun equipped(uuid: UUID): Title? = equipped[uuid]?.let { titles[it] }

    fun equip(uuid: UUID, id: String): Boolean {
        val key = id.lowercase()
        if (!isGranted(uuid, key)) return false
        equipped[uuid] = key
        save()
        return true
    }

    fun unequip(uuid: UUID): Boolean {
        val removed = equipped.remove(uuid) != null
        if (removed) save()
        return removed
    }

    fun load() {
        titles.clear()
        granted.clear()
        equipped.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        yaml.getConfigurationSection("titles")?.getKeys(false)?.forEach { key ->
            val section = yaml.getConfigurationSection("titles.$key") ?: return@forEach
            val display = section.getString("display") ?: return@forEach
            val id = section.getString("id") ?: key
            titles[key] = Title(id, display)
        }

        yaml.getConfigurationSection("players")?.getKeys(false)?.forEach { key ->
            val uuid = runCatching { UUID.fromString(key) }.getOrNull() ?: return@forEach
            val section = yaml.getConfigurationSection("players.$key") ?: return@forEach
            val grantedIds = section.getStringList("granted").map { it.lowercase() }.toMutableSet()
            if (grantedIds.isNotEmpty()) granted[uuid] = grantedIds
            section.getString("equipped")?.let { equippedId ->
                if (grantedIds.contains(equippedId.lowercase())) equipped[uuid] = equippedId.lowercase()
            }
        }

        logger.info("Loaded ${titles.size} title(s).")
    }

    fun save() {
        val yaml = YamlConfiguration()
        titles.forEach { (key, title) ->
            yaml.set("titles.$key.id", title.id)
            yaml.set("titles.$key.display", title.display)
        }
        val uuids = granted.keys + equipped.keys
        uuids.forEach { uuid ->
            val base = "players.$uuid"
            granted[uuid]?.let { yaml.set("$base.granted", it.toList()) }
            equipped[uuid]?.let { yaml.set("$base.equipped", it) }
        }
        dataFolder.mkdirs()
        yaml.save(file)
    }
}
