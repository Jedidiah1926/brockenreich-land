package com.brockenreich.landplugin.farm

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File

/**
 * Tracks every block planted from an animated farm item and, once its fixed grow duration has
 * elapsed (real time, persisted across restarts - not random-tick-driven, since this server runs
 * with randomTickSpeed 0), forces it to full growth in one step rather than the usual gradual
 * per-tick advancement. Along the way, a block whose BlockData is Ageable (wheat, carrots,
 * potatoes, beetroots, nether wart, cocoa, sweet berries, melon/pumpkin stems - not mushrooms or
 * saplings, which don't have a texture-bearing age) has its age stepped up proportionally to
 * elapsed time against its own maximumAge (so a 7-stage crop like a melon/pumpkin stem visibly
 * uses all 7 of its intermediate textures, not just the 3 an evenly-spaced 4-checkpoint scheme
 * would give it), instead of sitting frozen until the single instant it finishes.
 */
class FarmManager(private val plugin: Plugin, private val dataFolder: File) {

    private data class Key(val world: String, val x: Int, val y: Int, val z: Int)

    /** [appliedAge] is the highest age this entry has already written to its block, to avoid redundant block updates. */
    private data class Entry(val type: FarmCropType, var plantedAt: Long, var dueAt: Long, var appliedAge: Int)

    private val file = File(dataFolder, "farm.yml")
    private val entries = mutableMapOf<Key, Entry>()
    private val growthMillis = mutableMapOf<FarmCropType, Long>()

    /** Live countdown holograms toggled on via shift-right-click - purely cosmetic, not persisted across restarts. */
    private val displays = mutableMapOf<Key, TextDisplay>()

    /** Set by /farm time test|default - a non-null value overrides every crop's configured duration for new plantings. */
    private var testDurationMillis: Long? = null

    private fun keyOf(block: Block) = Key(block.world.name, block.x, block.y, block.z)

    fun loadConfig(config: FileConfiguration) {
        growthMillis.clear()
        FarmCropType.entries.filter { it.autoGrows }.forEach { type ->
            val minutes = config.getDouble("farm.growthMinutes.${type.label}", type.defaultMinutes)
            growthMillis[type] = (minutes * 60_000.0).toLong()
        }
    }

    /** [seconds] null restores each crop's configured duration; a value overrides all of them for plantings from now on. */
    fun setTestSeconds(seconds: Long?) {
        testDurationMillis = seconds?.let { it * 1000L }
    }

    fun isTestMode(): Boolean = testDurationMillis != null

    private fun durationFor(type: FarmCropType): Long? = testDurationMillis ?: growthMillis[type]

    fun plant(block: Block, type: FarmCropType) {
        val duration = durationFor(type) ?: return
        val now = System.currentTimeMillis()
        entries[keyOf(block)] = Entry(type, now, now + duration, appliedAge = 0)
        save()
    }

    /**
     * Shift-right-click toggle: spawns a floating name-tag-style hologram 1.5 blocks above
     * [block] showing its live remaining time (updated every tick() second), or removes it if one
     * is already showing there. Returns false (nothing done) if [block] isn't a tracked planting.
     */
    fun toggleCountdownDisplay(block: Block): Boolean {
        val key = keyOf(block)
        val entry = entries[key] ?: return false

        val existing = displays.remove(key)
        if (existing != null) {
            existing.remove()
            return true
        }

        val display = block.world.spawn(block.location.add(0.5, 1.5, 0.5), TextDisplay::class.java)
        display.billboard = Display.Billboard.CENTER
        display.isPersistent = false
        display.text = formatRemaining(entry, System.currentTimeMillis())
        displays[key] = display
        return true
    }

    // Shows the top two units only (day+hour, hour+minute, minute+second), except once only
    // seconds are left - a multi-day timer like a 2x2 jungle tree's doesn't need minute/second
    // precision, and a near-finished one doesn't need to show "0분" alongside the seconds.
    private fun formatRemaining(entry: Entry, now: Long): String {
        val totalSeconds = ((entry.dueAt - now) / 1000).coerceAtLeast(0)
        val days = totalSeconds / 86_400
        val hours = (totalSeconds % 86_400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val text = when {
            days > 0 -> "${days}일 ${hours}시간"
            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            else -> "${seconds}초"
        }
        return "§f$text"
    }

    /**
     * Stops tracking [block] entirely - its growth timer and any countdown hologram showing over
     * it. Call this whenever a block that might be a tracked planting is destroyed some other way
     * (broken, exploded, etc.) before its timer finished, so a removed crop doesn't keep counting
     * down toward growing something that's no longer there, and doesn't leave a hologram floating
     * in mid-air with nothing under it. A no-op if [block] wasn't tracked.
     */
    fun cancelTracking(block: Block) {
        val key = keyOf(block)
        if (entries.remove(key) != null) {
            save()
        }
        displays.remove(key)?.remove()
    }

    fun start() {
        object : BukkitRunnable() {
            override fun run() = tick()
        }.runTaskTimer(plugin, 20L, 20L)
    }

    private fun tick() {
        if (entries.isEmpty()) return
        val now = System.currentTimeMillis()
        var changed = false

        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val (key, entry) = iterator.next()

            val world = Bukkit.getWorld(key.world)
            if (world == null) {
                iterator.remove()
                displays.remove(key)?.remove()
                changed = true
                continue
            }
            // Don't force a chunk load just to update a timer - it'll be picked up on a later
            // tick once the chunk is loaded again (the entry stays pending, nothing is lost).
            if (!world.isChunkLoaded(key.x shr 4, key.z shr 4)) continue
            val block = world.getBlockAt(key.x, key.y, key.z)

            if (entry.dueAt <= now) {
                forceFullyGrown(block, entry.type)
                // A structure-growth crop (sapling/mushroom family) can still fail after 40
                // bonemeal attempts if it's genuinely obstructed (not enough open space for a
                // tree, or - for mushrooms - any block at all in the way, not just logs); the
                // simple Ageable crops and melon/pumpkin's direct swap always succeed here, so
                // this only ever fires for the structure-growth types. A failure is final here -
                // no retry is scheduled, so a blocked sapling/mushroom just stays as-is forever
                // (clearing the obstruction afterward does nothing; it would need replanting).
                if (isStillGrowing(block, entry.type)) {
                    failGrowth(block)
                } else {
                    celebrateGrowth(block)
                }
                iterator.remove()
                displays.remove(key)?.remove()
                changed = true
                continue
            }

            displays[key]?.text = formatRemaining(entry, now)

            if (advanceProportionally(block, entry, now)) {
                changed = true
            }
        }
        if (changed) save()
    }

    /** Steps an Ageable crop's age up to match how much of its growth duration has elapsed so far. */
    private fun advanceProportionally(block: Block, entry: Entry, now: Long): Boolean {
        val data = block.blockData as? Ageable ?: return false
        val total = (entry.dueAt - entry.plantedAt).coerceAtLeast(1)
        val elapsed = (now - entry.plantedAt).coerceAtLeast(0)
        val fraction = (elapsed.toDouble() / total).coerceIn(0.0, 1.0)
        // maximumAge itself is reserved for forceFullyGrown at completion, not this proportional step.
        val targetAge = (fraction * data.maximumAge).toInt().coerceIn(0, data.maximumAge - 1)
        if (targetAge <= entry.appliedAge) return false
        data.age = targetAge
        block.blockData = data
        entry.appliedAge = targetAge
        return true
    }

    private fun celebrateGrowth(block: Block) {
        block.world.spawnParticle(Particle.HAPPY_VILLAGER, block.location.add(0.5, 0.5, 0.5), 20, 0.3, 0.4, 0.3, 0.0)
    }

    private fun failGrowth(block: Block) {
        block.world.spawnParticle(Particle.SMOKE, block.location.add(0.5, 0.5, 0.5), 20, 0.3, 0.4, 0.3, 0.02)
    }

    private fun isStillGrowing(block: Block, type: FarmCropType): Boolean = when (type) {
        FarmCropType.MUSHROOM ->
            block.type == Material.RED_MUSHROOM || block.type == Material.BROWN_MUSHROOM ||
                block.type == Material.CRIMSON_FUNGUS || block.type == Material.WARPED_FUNGUS
        FarmCropType.SAPLING, FarmCropType.JUNGLE_SAPLING, FarmCropType.JUNGLE_BIG_TREE, FarmCropType.DARK_OAK_SAPLING ->
            block.type.name.endsWith("_SAPLING") || block.type == Material.MANGROVE_PROPAGULE ||
                block.type == Material.AZALEA || block.type == Material.FLOWERING_AZALEA
        else -> (block.blockData as? Ageable)?.let { it.age < it.maximumAge } ?: false
    }

    // Repeatedly simulating bonemeal (rather than hand-setting block data) reuses vanilla's own
    // growth logic for every crop shape, including odd ones like a sapling's tree generation,
    // instead of reimplementing each one's rules by hand. Most crops finish in 1-2 calls; the
    // probabilistic ones (mushroom/sapling) are looped up to 40 times so a bad string of rolls
    // essentially never leaves growth incomplete. This also means a dark oak/jungle "big tree"
    // (a 2x2 sapling arrangement) needs no special handling here at all: each of the 4 corner
    // saplings is tracked and timed independently, but whichever one's timer fires first calls
    // real vanilla bonemeal on a block that - by then - already has its 3 neighbors planted, so
    // vanilla itself recognizes the complete 2x2 layout and grows the giant tree; the other three
    // corners' still-pending entries simply find a non-sapling block (already consumed into the
    // tree) once their own timers fire and no-op.
    private fun forceFullyGrown(block: Block, type: FarmCropType) {
        // Melon/pumpkin don't spawn their fruit in the usual adjacent block here - the stem's own
        // position is replaced by the fruit outright, so there's no leftover attached-stem block
        // that could keep trying (and failing) to grow further.
        when (type) {
            FarmCropType.MELON -> { block.type = Material.MELON; return }
            FarmCropType.PUMPKIN -> { block.type = Material.PUMPKIN; return }
            else -> {}
        }
        var attempts = 0
        while (attempts < 40 && isStillGrowing(block, type)) {
            block.applyBoneMeal(BlockFace.UP)
            attempts++
        }
    }

    fun load() {
        entries.clear()
        if (!file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        yaml.getConfigurationSection("entries")?.getKeys(false)?.forEach { id ->
            val section = yaml.getConfigurationSection("entries.$id") ?: return@forEach
            val world = section.getString("world") ?: return@forEach
            val type = FarmCropType.entries.firstOrNull { it.name == section.getString("type") } ?: return@forEach
            val key = Key(world, section.getInt("x"), section.getInt("y"), section.getInt("z"))
            val plantedAt = section.getLong("plantedAt")
            val dueAt = section.getLong("dueAt")
            val appliedAge = section.getInt("appliedAge")
            entries[key] = Entry(type, plantedAt, dueAt, appliedAge)
        }
    }

    fun save() {
        val yaml = YamlConfiguration()
        entries.entries.forEachIndexed { index, (key, entry) ->
            val base = "entries.$index"
            yaml.set("$base.world", key.world)
            yaml.set("$base.x", key.x)
            yaml.set("$base.y", key.y)
            yaml.set("$base.z", key.z)
            yaml.set("$base.type", entry.type.name)
            yaml.set("$base.plantedAt", entry.plantedAt)
            yaml.set("$base.dueAt", entry.dueAt)
            yaml.set("$base.appliedAge", entry.appliedAge)
        }
        dataFolder.mkdirs()
        yaml.save(file)
    }
}
