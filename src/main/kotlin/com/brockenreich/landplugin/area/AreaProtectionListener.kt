package com.brockenreich.landplugin.area

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.LingeringPotionSplashEvent
import org.bukkit.event.entity.PotionSplashEvent
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

    // Pistons (including sticky pistons dragging slime/honey block structures) must not push or
    // pull any block across a PISTON-protected area boundary. event.blocks already contains every
    // block that will move - including ones dragged in sideways via slime/honey adhesion - and
    // they all translate by the same `direction`, so checking each one individually is sufficient.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (crossesPistonProtectedBoundary(event.blocks, event.direction)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (crossesPistonProtectedBoundary(event.blocks, event.direction)) {
            event.isCancelled = true
        }
    }

    private fun crossesPistonProtectedBoundary(blocks: List<Block>, direction: BlockFace): Boolean =
        blocks.any { block ->
            val from = block.location
            val to = from.clone().add(direction.modX.toDouble(), direction.modY.toDouble(), direction.modZ.toDouble())
            val fromArea = areaManager.areaAt(from)
            val toArea = areaManager.areaAt(to)
            fromArea !== toArea &&
                (fromArea.protections.contains(AreaProtection.PISTON) || toArea.protections.contains(AreaProtection.PISTON))
        }

    // Stops water/lava that originates inside a FLOOD-protected area from spreading past its
    // boundary. Only the source area's setting matters - liquid is free to flow in from outside.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        if (!event.block.isLiquid) return
        val fromArea = areaManager.areaAt(event.block.location)
        if (!fromArea.protections.contains(AreaProtection.FLOOD)) return
        val toArea = areaManager.areaAt(event.toBlock.location)
        if (toArea !== fromArea) {
            event.isCancelled = true
        }
    }

    // Splash potions: exclude only the entities standing inside a POTION-protected area, rather
    // than cancelling the whole splash (entities outside the boundary are still affected normally).
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPotionSplash(event: PotionSplashEvent) {
        event.affectedEntities.forEach { entity ->
            if (areaManager.areaAt(entity.location).protections.contains(AreaProtection.POTION)) {
                event.setIntensity(entity, 0.0)
            }
        }
    }

    // Lingering potions: if the potion breaks inside a POTION-protected area, don't let the
    // residual area-effect cloud form there at all.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onLingeringPotionSplash(event: LingeringPotionSplashEvent) {
        if (areaManager.areaAt(event.entity.location).protections.contains(AreaProtection.POTION)) {
            event.isCancelled = true
        }
    }

    // TNT, end crystals, creepers, withers, etc. Protects block by block, so an explosion
    // straddling the boundary still damages the unprotected side normally.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeIf { block ->
            areaManager.areaAt(block.location).protections.contains(AreaProtection.EXPLOSION)
        }
    }

    // Beds/respawn anchors exploding outside their valid dimension.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeIf { block ->
            areaManager.areaAt(block.location).protections.contains(AreaProtection.EXPLOSION)
        }
    }

    // Explosion damage to entities/players standing inside an EXPLOSION_DAMAGE-protected area.
    // Kept independent from EXPLOSION (block destruction) so either can be toggled on its own.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onExplosionDamage(event: EntityDamageEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION &&
            event.cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
        ) {
            return
        }
        if (areaManager.areaAt(event.entity.location).protections.contains(AreaProtection.EXPLOSION_DAMAGE)) {
            event.isCancelled = true
        }
    }
}
