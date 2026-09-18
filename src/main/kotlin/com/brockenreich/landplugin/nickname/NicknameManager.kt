package com.brockenreich.landplugin.nickname

import com.brockenreich.landplugin.util.parseUuidOrNull
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

class NicknameManager(private val dataFolder: File, private val logger: Logger) {

    private val file = File(dataFolder, "nicknames.yml")
    private val nicknames: MutableMap<UUID, String> = mutableMapOf()

    fun nickname(uuid: UUID): String? = nicknames[uuid]

    /** True if [nickname] is already some *other* player's current nickname. */
    fun isTaken(nickname: String, exclude: UUID): Boolean =
        nicknames.any { (uuid, name) -> uuid != exclude && name.equals(nickname, ignoreCase = true) }

    fun set(uuid: UUID, nickname: String) {
        nicknames[uuid] = nickname
        save()
    }

    fun reset(uuid: UUID): Boolean {
        val removed = nicknames.remove(uuid) != null
        if (removed) save()
        return removed
    }

    fun load() {
        nicknames.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getConfigurationSection("nicknames")?.getKeys(false)?.forEach { key ->
            val uuid = parseUuidOrNull(key) ?: return@forEach
            val nickname = yaml.getString("nicknames.$key") ?: return@forEach
            nicknames[uuid] = nickname
        }
        logger.info("Loaded ${nicknames.size} nickname(s).")
    }

    fun save() {
        val yaml = YamlConfiguration()
        nicknames.forEach { (uuid, nickname) -> yaml.set("nicknames.$uuid", nickname) }
        dataFolder.mkdirs()
        yaml.save(file)
    }
}
