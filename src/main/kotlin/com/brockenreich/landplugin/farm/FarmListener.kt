package com.brockenreich.landplugin.farm

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class FarmListener(private val farmManager: FarmManager, private val farmItems: FarmItems) : Listener {

    // Only an item enchanted via /farm animate may be planted as one of the 14 tracked farm
    // crops - an un-enchanted seed/sapling/etc. simply refuses to go in the ground at all.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val type = FarmCropType.forItem(event.itemInHand.type) ?: return
        if (!farmItems.isAnimated(event.itemInHand)) {
            event.isCancelled = true
            event.player.sendMessage("§c인챈트되지 않은 씨앗/모종은 심을 수 없습니다. /farm animate 로 먼저 인챈트하세요.")
            return
        }
        if (type.autoGrows) {
            farmManager.plant(event.blockPlaced, type)
        }
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

    // Shift-right-clicking a still-growing farm crop shows the remaining fixed grow time as a
    // title, instead of doing whatever that right click would normally do to the crop.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onCheckGrowth(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand == EquipmentSlot.OFF_HAND) return
        if (!event.player.isSneaking) return
        val block = event.clickedBlock ?: return
        val remaining = farmManager.remainingSeconds(block) ?: return

        event.isCancelled = true
        val minutes = remaining / 60
        val seconds = remaining % 60
        event.player.showTitle(Title.title(Component.text("${minutes}분 ${seconds}초"), Component.empty()))
    }
}
