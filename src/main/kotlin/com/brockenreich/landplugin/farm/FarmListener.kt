package com.brockenreich.landplugin.farm

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class FarmListener(private val farmManager: FarmManager, private val farmItems: FarmItems) : Listener {

    companion object {
        private val SUGAR_CANE_BASES = setOf(
            Material.SAND, Material.RED_SAND, Material.DIRT, Material.GRASS_BLOCK,
            Material.PODZOL, Material.COARSE_DIRT, Material.MYCELIUM, Material.MUD,
            Material.MOSS_BLOCK, Material.ROOTED_DIRT
        )
    }

    // Only an item enchanted via /farm animate may be planted as one of the tracked farm crops -
    // an un-enchanted seed/sapling/etc. simply refuses to go in the ground at all.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val type = FarmCropType.forItem(event.itemInHand.type) ?: return
        if (!farmItems.isAnimated(event.itemInHand)) {
            event.isCancelled = true
            event.player.sendMessage("§c인챈트되지 않은 씨앗/모종은 심을 수 없습니다. /farm animate 로 먼저 인챈트하세요.")
            return
        }
        when (type) {
            FarmCropType.JUNGLE_SAPLING -> handleJunglePlacement(event.blockPlaced)
            FarmCropType.DARK_OAK_SAPLING -> handleDarkOakPlacement(event.blockPlaced)
            else -> if (type.autoGrows) farmManager.plant(event.blockPlaced, type)
        }
    }

    // A jungle sapling grows fine alone (the lone JUNGLE_SAPLING duration), but if this placement
    // completes a 2x2 of jungle saplings, all 4 corners - including ones already ticking down on
    // the lone duration - are rescheduled together as a JUNGLE_BIG_TREE instead.
    private fun handleJunglePlacement(block: Block) {
        val group = findCompleted2x2(block, Material.JUNGLE_SAPLING)
        if (group != null) {
            group.forEach { farmManager.plant(it, FarmCropType.JUNGLE_BIG_TREE) }
        } else {
            farmManager.plant(block, FarmCropType.JUNGLE_SAPLING)
        }
    }

    // Dark oak can never grow as a lone sapling in vanilla - only as a 2x2. Scheduling a growth
    // timer for an incomplete corner would just waste forceFullyGrown's bonemeal attempts on a
    // sapling that can never actually grow, so a corner stays untracked (and simply sits there,
    // same as vanilla) until this placement completes the group for all 4 at once.
    private fun handleDarkOakPlacement(block: Block) {
        val group = findCompleted2x2(block, Material.DARK_OAK_SAPLING)
        group?.forEach { farmManager.plant(it, FarmCropType.DARK_OAK_SAPLING) }
    }

    /** If [block] (already placed, of [species]) completes a 2x2 group of that species at its Y level, the 4 blocks; else null. */
    private fun findCompleted2x2(block: Block, species: Material): List<Block>? {
        for (dx in -1..0) {
            for (dz in -1..0) {
                val corners = (0..1).flatMap { ix ->
                    (0..1).map { iz -> block.world.getBlockAt(block.x + dx + ix, block.y, block.z + dz + iz) }
                }
                if (corners.all { it.type == species }) return corners
            }
        }
        return null
    }

    // Farmland mined by a player drops itself (not vanilla's dirt) so it can be replanted
    // directly without a hoe.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBreakFarmland(event: BlockBreakEvent) {
        if (event.block.type != Material.FARMLAND) return
        if (event.player.gameMode == GameMode.CREATIVE) return
        event.isDropItems = false
        event.block.world.dropItemNaturally(event.block.location.add(0.5, 0.1, 0.5), ItemStack(Material.FARMLAND))
    }

    // Farmland placed by a player starts fully moist (the dark, wet-looking texture) instead of
    // vanilla's dry default - moisture would only ever rise on its own via random ticks anyway,
    // which never happen with randomTickSpeed 0.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlaceFarmland(event: BlockPlaceEvent) {
        if (event.blockPlaced.type != Material.FARMLAND) return
        val data = event.blockPlaced.blockData as? Levelled ?: return
        data.level = data.maximumLevel
        event.blockPlaced.blockData = data
    }

    // Farmland never reverts to dirt on this server, whether from a mob/player trampling it...
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onTrample(event: EntityChangeBlockEvent) {
        if (event.block.type == Material.FARMLAND && event.to == Material.DIRT) {
            event.isCancelled = true
        }
    }

    // ...or from drying out / being blocked from light by a block placed on top of it.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFade(event: BlockFadeEvent) {
        if (event.block.type == Material.FARMLAND && event.newState.type == Material.DIRT) {
            event.isCancelled = true
        }
    }

    // Shift-right-clicking a still-growing farm crop toggles a floating, name-tag-style hologram
    // 1.5 blocks above it showing the live remaining grow time (ticking down in real time) -
    // right-clicking it again while sneaking removes it. Does nothing for a block that isn't a
    // tracked planting, letting whatever right click would normally do proceed instead.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onCheckGrowth(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand == EquipmentSlot.OFF_HAND) return
        if (!event.player.isSneaking) return
        val block = event.clickedBlock ?: return
        if (farmManager.toggleCountdownDisplay(block)) {
            event.isCancelled = true
        }
    }

    // Sugar cane normally refuses to even be placed away from water. An animated sugar cane item
    // bypasses vanilla's placement check entirely here (vanilla never fires BlockPlaceEvent for a
    // rejected placement, so this has to happen at the raw interact level) and places the block
    // by hand instead - still requiring a sane ground block underneath, just not water nearby.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlaceSugarCaneAnywhere(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand == EquipmentSlot.OFF_HAND) return
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type != Material.SUGAR_CANE) return
        if (!farmItems.isAnimated(item)) return

        val clicked = event.clickedBlock ?: return
        val face = event.blockFace
        val target = clicked.getRelative(face)
        if (!target.type.isAir) return

        val base = target.getRelative(BlockFace.DOWN)
        if (base.type != Material.SUGAR_CANE && base.type !in SUGAR_CANE_BASES) return

        event.isCancelled = true
        target.type = Material.SUGAR_CANE
        target.world.playSound(target.location, Sound.BLOCK_GRASS_PLACE, 1f, 1f)
        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
    }

    // Sugar cane must survive without adjacent water on this server - cancelling its physics
    // check stops vanilla from ever popping it off for "missing" that support.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSugarCanePhysics(event: BlockPhysicsEvent) {
        if (event.block.type == Material.SUGAR_CANE) {
            event.isCancelled = true
        }
    }

    // Sweet berries are the one farm crop that's harvested without breaking the plant (vanilla
    // drops berries and drops the bush's age back down instead of removing it) - so unlike every
    // other tracked crop, it can be picked over and over. Each harvest restarts its own fixed
    // growth timer so it regrows to full over the same duration again.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHarvestSweetBerry(event: PlayerHarvestBlockEvent) {
        if (event.harvestedBlock.type != Material.SWEET_BERRY_BUSH) return
        farmManager.plant(event.harvestedBlock, FarmCropType.SWEET_BERRY)
    }
}
