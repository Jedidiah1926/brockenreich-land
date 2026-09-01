package com.brockenreich.landplugin.area

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID
import java.util.logging.Logger

class AreaManager(private val dataFolder: File, private val logger: Logger) {

    private val file = File(dataFolder, "areas.yml")
    private val regions: MutableMap<String, Area> = mutableMapOf()
    private val worldAreas: MutableMap<String, Area> = mutableMapOf()

    fun region(name: String): Area? = regions[name.lowercase()]

    fun regions(): Collection<Area> = regions.values

    /** World areas that have been explicitly registered or touched (not every loaded Bukkit world). */
    fun worldAreas(): Collection<Area> = worldAreas.values

    fun worldArea(world: String): Area =
        worldAreas.getOrPut(world.lowercase()) { Area(AreaTarget.WorldArea(world), world) }

    /** Clears any explicit member/permission overrides for [world], reverting it to the open default. */
    fun resetWorldArea(world: String): Boolean {
        val removed = worldAreas.remove(world.lowercase()) != null
        if (removed) save()
        return removed
    }

    fun area(target: AreaTarget): Area = when (target) {
        is AreaTarget.WorldArea -> worldArea(target.world)
        is AreaTarget.Region -> regions[target.name.lowercase()]
            ?: throw IllegalArgumentException("존재하지 않는 구역입니다: ${target.name}")
    }

    fun createRegion(name: String, min: Location, max: Location): Area {
        val key = name.lowercase()
        require(!regions.containsKey(key)) { "이미 존재하는 구역 이름입니다: $name" }
        val world = min.world?.name ?: error("월드를 알 수 없습니다.")
        val area = Area(AreaTarget.Region(name), world)
        area.min = min
        area.max = max
        regions[key] = area
        save()
        return area
    }

    fun deleteRegion(name: String): Boolean {
        val removed = regions.remove(name.lowercase()) != null
        if (removed) save()
        return removed
    }

    /** The most specific area covering [location]: a region if one contains it, else the world's catch-all area. */
    fun areaAt(location: Location): Area {
        val region = regions.values.firstOrNull { it.contains(location) }
        return region ?: worldArea(location.world?.name ?: "world")
    }

    fun load() {
        regions.clear()
        worldAreas.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)

        yaml.getConfigurationSection("regions")?.getKeys(false)?.forEach { key ->
            val section = yaml.getConfigurationSection("regions.$key") ?: return@forEach
            val worldName = section.getString("world") ?: return@forEach
            val world = Bukkit.getWorld(worldName)
            val minSec = section.getConfigurationSection("min") ?: return@forEach
            val maxSec = section.getConfigurationSection("max") ?: return@forEach
            val min = Location(world, minSec.getDouble("x"), minSec.getDouble("y"), minSec.getDouble("z"))
            val max = Location(world, maxSec.getDouble("x"), maxSec.getDouble("y"), maxSec.getDouble("z"))

            val area = Area(AreaTarget.Region(key), worldName)
            area.min = min
            area.max = max
            area.members.addAll(
                section.getStringList("members").mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            )
            area.admins.addAll(
                section.getStringList("admins").mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            )
            area.permissions.clear()
            area.permissions.addAll(section.getStringList("permissions").mapNotNull { AreaPermission.parse(it) })
            loadPlayerPermissions(section, area)
            area.protections.addAll(section.getStringList("protections").mapNotNull { AreaProtection.parse(it) })
            regions[key] = area
        }

        yaml.getConfigurationSection("worlds")?.getKeys(false)?.forEach { key ->
            val section = yaml.getConfigurationSection("worlds.$key") ?: return@forEach
            val area = worldArea(key)
            area.members.addAll(
                section.getStringList("members").mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            )
            area.admins.addAll(
                section.getStringList("admins").mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            )
            area.permissions.clear()
            area.permissions.addAll(section.getStringList("permissions").mapNotNull { AreaPermission.parse(it) })
            loadPlayerPermissions(section, area)
            area.protections.addAll(section.getStringList("protections").mapNotNull { AreaProtection.parse(it) })
        }

        logger.info("Loaded ${regions.size} region(s) and ${worldAreas.size} world area override(s).")
    }

    private fun loadPlayerPermissions(section: org.bukkit.configuration.ConfigurationSection, area: Area) {
        area.playerPermissions.clear()
        val playersSection = section.getConfigurationSection("playerPermissions") ?: return
        playersSection.getKeys(false).forEach { uuidKey ->
            val uuid = runCatching { UUID.fromString(uuidKey) }.getOrNull() ?: return@forEach
            val perms = playersSection.getStringList(uuidKey).mapNotNull { AreaPermission.parse(it) }.toMutableSet()
            if (perms.isNotEmpty()) {
                area.playerPermissions[uuid] = perms
            }
        }
    }

    fun save() {
        val yaml = YamlConfiguration()

        regions.forEach { (key, area) ->
            val base = "regions.$key"
            yaml.set("$base.world", area.world)
            area.min?.let {
                yaml.set("$base.min.x", it.x)
                yaml.set("$base.min.y", it.y)
                yaml.set("$base.min.z", it.z)
            }
            area.max?.let {
                yaml.set("$base.max.x", it.x)
                yaml.set("$base.max.y", it.y)
                yaml.set("$base.max.z", it.z)
            }
            yaml.set("$base.members", area.members.map { it.toString() })
            yaml.set("$base.admins", area.admins.map { it.toString() })
            yaml.set("$base.permissions", area.permissions.map { it.name })
            area.playerPermissions.forEach { (uuid, perms) ->
                yaml.set("$base.playerPermissions.$uuid", perms.map { it.name })
            }
            yaml.set("$base.protections", area.protections.map { it.name })
        }

        worldAreas.forEach { (key, area) ->
            val base = "worlds.$key"
            yaml.set("$base.members", area.members.map { it.toString() })
            yaml.set("$base.admins", area.admins.map { it.toString() })
            yaml.set("$base.permissions", area.permissions.map { it.name })
            area.playerPermissions.forEach { (uuid, perms) ->
                yaml.set("$base.playerPermissions.$uuid", perms.map { it.name })
            }
            yaml.set("$base.protections", area.protections.map { it.name })
        }

        dataFolder.mkdirs()
        yaml.save(file)
    }
}
