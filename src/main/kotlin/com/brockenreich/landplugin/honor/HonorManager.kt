package com.brockenreich.landplugin.honor

import com.brockenreich.landplugin.util.parseUuidOrNull
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

class HonorManager(private val dataFolder: File, private val logger: Logger) {

    private val file = File(dataFolder, "honors.yml")
    private val honors: MutableMap<String, Honor> = mutableMapOf()
    private val granted: MutableMap<UUID, MutableSet<String>> = mutableMapOf()
    private val equipped: MutableMap<UUID, String> = mutableMapOf()
    /** Players an OP has force-set via /honor force - they can't equip/unequip themselves while locked. */
    private val locked: MutableSet<UUID> = mutableSetOf()

    fun honor(id: String): Honor? = honors[id.lowercase()]

    fun honors(): Collection<Honor> = honors.values

    fun createHonor(id: String, display: String): Honor {
        val key = id.lowercase()
        require(!honors.containsKey(key)) { "이미 존재하는 칭호 ID입니다: $id" }
        val honor = Honor(id, display)
        honors[key] = honor
        save()
        return honor
    }

    fun deleteHonor(id: String): Boolean {
        val key = id.lowercase()
        val removed = honors.remove(key) != null
        if (removed) {
            granted.values.forEach { it.remove(key) }
            equipped.entries.removeIf { it.value == key }
            save()
        }
        return removed
    }

    fun isGranted(uuid: UUID, id: String): Boolean = granted[uuid]?.contains(id.lowercase()) ?: false

    /** Honor IDs (lowercase) [uuid] currently owns. */
    fun grantedTo(uuid: UUID): Set<String> = granted[uuid] ?: emptySet()

    fun grant(uuid: UUID, id: String): Boolean {
        val key = id.lowercase()
        if (!honors.containsKey(key)) return false
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

    fun equipped(uuid: UUID): Honor? = equipped[uuid]?.let { honors[it] }

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

    fun isLocked(uuid: UUID): Boolean = locked.contains(uuid)

    fun setLocked(uuid: UUID, value: Boolean) {
        if (value) locked.add(uuid) else locked.remove(uuid)
        save()
    }

    fun load() {
        honors.clear()
        granted.clear()
        equipped.clear()
        locked.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        yaml.getConfigurationSection("honors")?.getKeys(false)?.forEach { key ->
            val section = yaml.getConfigurationSection("honors.$key") ?: return@forEach
            val display = section.getString("display") ?: return@forEach
            val id = section.getString("id") ?: key
            honors[key] = Honor(id, display)
        }

        yaml.getConfigurationSection("players")?.getKeys(false)?.forEach { key ->
            val uuid = parseUuidOrNull(key) ?: return@forEach
            val section = yaml.getConfigurationSection("players.$key") ?: return@forEach
            val grantedIds = section.getStringList("granted").map { it.lowercase() }.toMutableSet()
            if (grantedIds.isNotEmpty()) granted[uuid] = grantedIds
            section.getString("equipped")?.let { equippedId ->
                if (grantedIds.contains(equippedId.lowercase())) equipped[uuid] = equippedId.lowercase()
            }
            if (section.getBoolean("locked")) locked.add(uuid)
        }

        logger.info("Loaded ${honors.size} honor(s).")
    }

    fun save() {
        val yaml = YamlConfiguration()
        honors.forEach { (key, honor) ->
            yaml.set("honors.$key.id", honor.id)
            yaml.set("honors.$key.display", honor.display)
        }
        val uuids = granted.keys + equipped.keys + locked
        uuids.forEach { uuid ->
            val base = "players.$uuid"
            granted[uuid]?.let { yaml.set("$base.granted", it.toList()) }
            equipped[uuid]?.let { yaml.set("$base.equipped", it) }
            if (locked.contains(uuid)) yaml.set("$base.locked", true)
        }
        dataFolder.mkdirs()
        yaml.save(file)
    }
}
