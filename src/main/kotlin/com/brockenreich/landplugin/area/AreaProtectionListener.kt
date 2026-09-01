package com.brockenreich.landplugin.area

import org.bukkit.Location
import org.bukkit.Material
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
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.entity.LingeringPotionSplashEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.StructureGrowEvent
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

    // Firework rockets aren't a Projectile, so they never reach onProjectileLaunch below. A
    // non-gliding use (right-click in the air or on a block to launch one) is gated behind
    // INTERACTION so it never even fires; boosting while gliding with an elytra is gated behind
    // PROJECTILE_LAUNCH instead, matching the semantics of a normal projectile launch.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFireworkUse(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand == EquipmentSlot.OFF_HAND) return
        if (event.item?.type != Material.FIREWORK_ROCKET) return

        val permission = if (event.player.isGliding) AreaPermission.PROJECTILE_LAUNCH else AreaPermission.INTERACTION
        if (denied(event.player, event.player.location, permission)) {
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

    // Pistons (including sticky pistons dragging slime/honey block structures, sideways-attached
    // ones included - see resolveMovingBlocks below) must not push or pull any block across a
    // PISTON-protected area boundary. Every moving block translates by the same `direction`, so
    // checking each one individually is sufficient.
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
        resolveMovingBlocks(blocks).any { block ->
            val from = block.location
            val to = from.clone().add(direction.modX.toDouble(), direction.modY.toDouble(), direction.modZ.toDouble())
            val fromArea = areaManager.areaAt(from)
            val toArea = areaManager.areaAt(to)
            fromArea !== toArea &&
                (fromArea.protections.contains(AreaProtection.PISTON) || toArea.protections.contains(AreaProtection.PISTON))
        }

    /**
     * Bukkit's BlockPistonExtendEvent/RetractEvent.getBlocks() has historically under-reported
     * blocks dragged in sideways via slime/honey block adhesion. Rather than trust it completely,
     * flood-fill outward from the reported blocks through any slime/honey adhesion in all six
     * directions, so a laterally-attached structure is never missed by the boundary check above.
     * Over-including a block here (treating it as "would move" when vanilla might not actually
     * move it) is a safe failure mode for a protection feature; under-including is not.
     */
    private fun resolveMovingBlocks(seeds: List<Block>): Set<Block> {
        val visited = LinkedHashSet<Block>()
        val queue = ArrayDeque(seeds)
        val faces = arrayOf(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
        while (queue.isNotEmpty() && visited.size < 256) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            if (current.type != Material.SLIME_BLOCK && current.type != Material.HONEY_BLOCK) continue
            for (face in faces) {
                val neighbor = current.getRelative(face)
                if (neighbor.type.isAir) continue
                if (neighbor !in visited) queue.add(neighbor)
            }
        }
        return visited
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

    // Splash potions: if the potion breaks inside a POTION-protected area, cancel the whole splash
    // (including its particle effect) rather than just neutralizing entities one by one. If it
    // breaks outside a protected area, keep the splash/particle there but still exclude any
    // affected entity that happens to be standing inside a protected area.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onPotionSplash(event: PotionSplashEvent) {
        if (areaManager.areaAt(event.entity.location).protections.contains(AreaProtection.POTION)) {
            event.isCancelled = true
            return
        }
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

    // A lingering cloud that formed outside a protected area can still grow and drift so its
    // radius reaches into one over time. This fires every time the cloud pulses and is about to
    // apply its effect, so re-checking each affected entity's current location here (rather than
    // only where the cloud was created) catches that case too.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) {
        event.affectedEntities.removeIf { entity ->
            areaManager.areaAt(entity.location).protections.contains(AreaProtection.POTION)
        }
    }

    // Fires before the explosion actually happens (sound/particles included), for any exploding
    // entity (TNT, creeper, end crystal, wither, fireballs, ...). If the entity itself is inside an
    // EXPLOSION-protected area, cancel the explosion outright - no sound, particles, damage, or
    // block loss anywhere. This is all-or-nothing (unlike the block-list filtering below): by the
    // time EntityExplodeEvent fires, the sound/particle packets have already been sent to nearby
    // clients, so there's no way to suppress those only for the protected side of a boundary that
    // an explosion straddles.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onExplosionPrime(event: ExplosionPrimeEvent) {
        if (areaManager.areaAt(event.entity.location).protections.contains(AreaProtection.EXPLOSION)) {
            event.isCancelled = true
        }
    }

    // Backstop for explosions that weren't primed inside a protected area but still reach one
    // (e.g. TNT exploding just outside the boundary). Protects block by block, so an explosion
    // straddling the boundary still damages the unprotected side normally - sound/particles from
    // that already-happened explosion play regardless, per the note above.
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

    // Trees, huge mushrooms, etc. Only filters when the sapling/origin block sits in an
    // OVERFLOW-protected area, dropping any resulting block (leaves, logs, ...) that would land in
    // a different area than that origin - so growth stays contained within whichever region or
    // world catch-all area it started in.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        val originArea = areaManager.areaAt(event.location)
        if (!originArea.protections.contains(AreaProtection.OVERFLOW)) return
        event.blocks.removeIf { blockState ->
            areaManager.areaAt(blockState.location) !== originArea
        }
    }
}
