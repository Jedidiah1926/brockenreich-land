package com.brockenreich.landplugin.area

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** Enforces the non-entrance/exit AreaPermission flags against the matching Bukkit events. */
class AreaProtectionListener(private val areaManager: AreaManager) : Listener {

    private fun denied(player: Player, location: Location, permission: AreaPermission): Boolean {
        if (player.hasPermission("brockenreichland.area.bypass")) return false
        return !areaManager.areaAt(location).can(player, permission)
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand == EquipmentSlot.OFF_HAND) return
        val block = event.clickedBlock ?: return
        if (denied(event.player, block.location, AreaPermission.INTERACTION)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (denied(player, event.item.location, AreaPermission.PICKUP_ITEM)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDropItem(event: PlayerDropItemEvent) {
        if (denied(event.player, event.player.location, AreaPermission.DROP_ITEM)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (denied(event.player, event.block.location, AreaPermission.BLOCK_BREAK)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (denied(event.player, event.block.location, AreaPermission.BLOCK_PLACE)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val player = event.player ?: return
        if (denied(player, event.block.location, AreaPermission.BLOCK_IGNITING)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        if (denied(player, event.entity.location, AreaPermission.HANGING_PLACE)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onHangingBreak(event: HangingBreakByEntityEvent) {
        val player = event.remover as? Player ?: return
        if (denied(player, event.entity.location, AreaPermission.HANGING_BREAK)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity as? Projectile ?: return
        val player = projectile.shooter as? Player ?: return
        if (denied(player, projectile.location, AreaPermission.PROJECTILE_LAUNCH)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val permission = if (event.entity is Player) AreaPermission.ATTACK_PLAYER else AreaPermission.ATTACK_ENTITY
        if (denied(attacker, event.entity.location, permission)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (denied(event.player, event.blockClicked.location, AreaPermission.BUCKET_EMPTY)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBucketFill(event: PlayerBucketFillEvent) {
        if (denied(event.player, event.blockClicked.location, AreaPermission.BUCKET_FILL)) {
            event.isCancelled = true
        }
    }
}
