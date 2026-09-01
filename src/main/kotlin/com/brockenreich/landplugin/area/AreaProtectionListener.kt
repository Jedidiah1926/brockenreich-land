package com.brockenreich.landplugin.area

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Directional
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
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
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.block.SpongeAbsorbEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerLeashEntityEvent
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

    // Right-clicking an entity (villager trading, feeding/petting animals, opening a minecart
    // chest, rotating an item frame's item, mounting a boat, etc.) was unguarded - a player
    // standing just outside a boundary within reach could freely interact with entities sitting
    // just inside it, regardless of that area's INTERACTION permission. Checked at the entity's
    // location, not the player's, matching onInteract's block-target check above.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand == EquipmentSlot.OFF_HAND) return
        if (denied(event.player, event.rightClicked.location, AreaPermission.INTERACTION)) {
            event.isCancelled = true
        }
    }

    // PlayerInteractAtEntityEvent (precise-click interactions, e.g. armor stands) has its own
    // handler list separate from PlayerInteractEntityEvent above despite being a subclass, so it
    // needs its own registration to actually be caught.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (event.hand == EquipmentSlot.OFF_HAND) return
        if (denied(event.player, event.rightClicked.location, AreaPermission.INTERACTION)) {
            event.isCancelled = true
        }
    }

    // PlayerArmorStandManipulateEvent (equipping/unequipping an armor stand's item slots) is a
    // subclass of PlayerInteractAtEntityEvent above but, like it, has its own separate handler
    // list - registered explicitly here so taking gear off an armor stand is gated the same way,
    // even if some code path fires this without the generic interact-at-entity event alongside it.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (denied(event.player, event.rightClicked.location, AreaPermission.INTERACTION)) {
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

    // Checked at both the attacker's own location and the victim's - a sweep attack (sword
    // swing hitting nearby entities) can land on someone standing just outside the boundary
    // while the attacker stays inside, which `denied(attacker, event.entity.location, ...)`
    // alone wouldn't catch since that only consults the permissions of the area the *victim*
    // is standing in. The damager is resolved through a projectile to its shooter too - an arrow
    // (or trident, snowball, ...) fired from outside a boundary that lands a hit inside it is
    // still the shooter's attack, not exempt just because event.damager is the Arrow entity.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = resolveAttackingPlayer(event.damager) ?: return
        val permission = if (event.entity is Player) AreaPermission.ATTACK_PLAYER else AreaPermission.ATTACK_ENTITY
        if (denied(attacker, attacker.location, permission) || denied(attacker, event.entity.location, permission)) {
            event.isCancelled = true
        }
    }

    private fun resolveAttackingPlayer(damager: Entity): Player? = when (damager) {
        is Player -> damager
        is Projectile -> damager.shooter as? Player
        else -> null
    }

    // Hooking an entity/player with a fishing rod and reeling it in is checked at both ends, same
    // as attackEntity/attackPlayer above - the hook can travel far enough to reach across a
    // boundary even while the angler stays put on one side of it.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFishPullEntity(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_ENTITY) return
        val caught = event.caught ?: return
        val player = event.player
        if (denied(player, player.location, AreaPermission.FISH_PULL) || denied(player, caught.location, AreaPermission.FISH_PULL)) {
            event.isCancelled = true
        }
    }

    // Leashing is checked at the leashed entity's own location, matching interaction/attack-style
    // target-location checks elsewhere.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onLeash(event: PlayerLeashEntityEvent) {
        if (denied(event.player, event.entity.location, AreaPermission.LEASH)) {
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

    // A dispenser shooting an arrow/potion/firework or squirting a bucket's liquid out its front
    // face must not do so across a DISPENSER-protected boundary, in either direction, while still
    // working normally for a target inside the same area.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDispense(event: BlockDispenseEvent) {
        val facing = (event.block.blockData as? Directional)?.facing ?: return
        val from = event.block.location
        val to = from.clone().add(facing.modX.toDouble(), facing.modY.toDouble(), facing.modZ.toDouble())
        val fromArea = areaManager.areaAt(from)
        val toArea = areaManager.areaAt(to)
        if (fromArea !== toArea &&
            (fromArea.protections.contains(AreaProtection.DISPENSER) || toArea.protections.contains(AreaProtection.DISPENSER))
        ) {
            event.isCancelled = true
        }
    }

    // A hopper (or dropper/hopper minecart feeding one) transferring an item between two
    // inventories must not do so across a HOPPER-protected boundary, in either direction, while
    // transfers within the same area keep working normally. Mirrors the dispenser check above.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        val from = event.source.location ?: return
        val to = event.destination.location ?: return
        val fromArea = areaManager.areaAt(from)
        val toArea = areaManager.areaAt(to)
        if (fromArea !== toArea &&
            (fromArea.protections.contains(AreaProtection.HOPPER) || toArea.protections.contains(AreaProtection.HOPPER))
        ) {
            event.isCancelled = true
        }
    }

    // Same idea for a hopper (or hopper minecart) vacuuming a loose item entity off the ground -
    // it must not reach across a HOPPER-protected boundary to do it either.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInventoryPickupItem(event: InventoryPickupItemEvent) {
        val invArea = areaManager.areaAt(event.inventory.location ?: return)
        val itemArea = areaManager.areaAt(event.item.location)
        if (invArea !== itemArea &&
            (invArea.protections.contains(AreaProtection.HOPPER) || itemArea.protections.contains(AreaProtection.HOPPER))
        ) {
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

    // Bonemeal on grass (spawning tall grass/flowers in a radius) doesn't go through
    // StructureGrowEvent above - it's a separate event, filtered the same way: only when the
    // fertilized block itself sits in an OVERFLOW-protected area, dropping any resulting block
    // that would land in a different area than that origin.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFertilize(event: BlockFertilizeEvent) {
        val originArea = areaManager.areaAt(event.block.location)
        if (!originArea.protections.contains(AreaProtection.OVERFLOW)) return
        event.blocks.removeIf { blockState ->
            areaManager.areaAt(blockState.location) !== originArea
        }
    }

    // A sponge sitting in a SPONGE-protected area must not reach out and absorb water sitting in
    // a different area - filtered the same way as overflow/fertilize: only the absorbed blocks
    // that land in the sponge's own area survive.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onSpongeAbsorb(event: SpongeAbsorbEvent) {
        val spongeArea = areaManager.areaAt(event.block.location)
        if (!spongeArea.protections.contains(AreaProtection.SPONGE)) return
        event.blocks.removeIf { blockState ->
            areaManager.areaAt(blockState.location) !== spongeArea
        }
    }

    // Redstone dust freezes its current whenever any of its six neighbors belongs to a different
    // area and either side has REDSTONE protection on - matching the either-side pattern used by
    // piston/dispenser/hopper elsewhere, rather than only ever checking the changing wire's own
    // area. That matters here specifically because BlockRedstoneEvent fires separately per wire
    // block in a network: a wire just outside a protected boundary needs its own event frozen too
    // (using the *protected* neighbor's flag) to actually stop a signal from leaking out, not just
    // the protected side's own event (which only stops it from leaking in). BlockRedstoneEvent is
    // NOT Cancellable - influence it only via setNewCurrent, and never add ignoreCancelled here.
    @EventHandler(priority = EventPriority.LOW)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        val wireArea = areaManager.areaAt(event.block.location)
        val faces = arrayOf(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
        val crossesBoundary = faces.any { face ->
            val neighborArea = areaManager.areaAt(event.block.getRelative(face).location)
            neighborArea !== wireArea &&
                (wireArea.protections.contains(AreaProtection.REDSTONE) || neighborArea.protections.contains(AreaProtection.REDSTONE))
        }
        if (crossesBoundary) {
            event.newCurrent = event.oldCurrent
        }
    }

    // FIRE guards ignition itself (flint & steel, lightning, lava, fireballs, ...) and FIRE_SPREAD
    // guards fire spreading on its own - a fire block spreading to a new one (BlockIgniteEvent with
    // cause SPREAD) and a flammable block being consumed by adjacent fire (BlockBurnEvent). Kept
    // independent of the blockIgniting AreaPermission above, which only governs player-caused
    // ignition; these apply regardless of cause.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockIgniteProtection(event: BlockIgniteEvent) {
        val area = areaManager.areaAt(event.block.location)
        val protection = if (event.cause == BlockIgniteEvent.IgniteCause.SPREAD) {
            AreaProtection.FIRE_SPREAD
        } else {
            AreaProtection.FIRE
        }
        if (area.protections.contains(protection)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        if (areaManager.areaAt(event.block.location).protections.contains(AreaProtection.FIRE_SPREAD)) {
            event.isCancelled = true
        }
    }

    // Any entity (not just players, who are separately gated by the pickupItem permission above)
    // picking up a dropped item while standing in an ENTITY_PICKUP_ITEM-protected area is blocked -
    // covers mobs like foxes/villagers scooping up drops inside a protected area.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityPickupItemProtection(event: EntityPickupItemEvent) {
        if (areaManager.areaAt(event.item.location).protections.contains(AreaProtection.ENTITY_PICKUP_ITEM)) {
            event.isCancelled = true
        }
    }

    // A general "safe zone" toggle: blocks non-attack, non-explosion damage (fall, fire, drowning,
    // starvation, void, ...) to anything standing inside a DAMAGE-protected area. Independent of
    // attackEntity/attackPlayer (player-caused, a separate Cancellable subclass this never sees)
    // and explosionDamage (explosion-caused specifically, likewise a separate handler).
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onDamageProtection(event: EntityDamageEvent) {
        if (areaManager.areaAt(event.entity.location).protections.contains(AreaProtection.DAMAGE)) {
            event.isCancelled = true
        }
    }

    // Any entity-caused block change - a sheep eating grass, an enderman placing/picking up a
    // block, a falling sand/gravel/anvil block landing and turning back into a solid block, a
    // wither/ravager destroying blocks, etc. - is blocked wherever it would land inside an
    // ENTITY_CHANGE_BLOCK-protected area (this is also how a falling block landing is caught, so
    // "protect this area from stuff dropped on it" works too).
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (areaManager.areaAt(event.block.location).protections.contains(AreaProtection.ENTITY_CHANGE_BLOCK)) {
            event.isCancelled = true
        }
    }

    // Grass/mycelium/vines/mushrooms/sculk/coral etc. spreading into an adjacent block - checked
    // at the spreading source, same pattern as overflow/sponge: only stops it from reaching a
    // different area than the one it started in.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        val sourceArea = areaManager.areaAt(event.source.location)
        if (!sourceArea.protections.contains(AreaProtection.BLOCK_SPREAD)) return
        if (areaManager.areaAt(event.block.location) !== sourceArea) {
            event.isCancelled = true
        }
    }

    // Ice/snow melting, coral dying, and any other natural fading inside a FADE-protected area
    // (farmland drying specifically is already prevented server-wide regardless of area - see
    // FarmListener - so this only ever matters for the other cases BlockFadeEvent covers).
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockFade(event: BlockFadeEvent) {
        if (areaManager.areaAt(event.block.location).protections.contains(AreaProtection.FADE)) {
            event.isCancelled = true
        }
    }

    // Ice/snow/concrete forming naturally inside a FORM-protected area.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockForm(event: BlockFormEvent) {
        if (areaManager.areaAt(event.block.location).protections.contains(AreaProtection.FORM)) {
            event.isCancelled = true
        }
    }

    // A crop naturally advancing a growth stage inside a GROWTH-protected area. This also applies
    // to the farm system's forced growth (it simulates real bonemeal, which fires this same
    // event), so a GROWTH-protected area blocks farm crops from completing there too.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onBlockGrow(event: BlockGrowEvent) {
        if (areaManager.areaAt(event.block.location).protections.contains(AreaProtection.GROWTH)) {
            event.isCancelled = true
        }
    }
}
