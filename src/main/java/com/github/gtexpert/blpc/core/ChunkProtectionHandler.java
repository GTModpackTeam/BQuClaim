package com.github.gtexpert.blpc.core;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.chunk.ChunkManagerData;
import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;

public class ChunkProtectionHandler {

    // --- Helper methods ---

    private static boolean isChunkClaimed(int chunkX, int chunkZ) {
        return ChunkManagerData.getInstance().getClaim(chunkX, chunkZ) != null;
    }

    @Nullable
    private static Party getPartyForClaim(ClaimedChunkData claim) {
        return PartyManagerData.getInstance().getPartyByPlayer(claim.ownerUUID);
    }

    private static boolean canPlayerActAt(@Nullable EntityPlayer player, BlockPos pos) {
        if (!ModConfig.enableProtection) return true;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ClaimedChunkData claim = ChunkManagerData.getInstance().getClaim(chunkX, chunkZ);

        if (claim == null) return true;
        if (player == null) return false;

        if (player instanceof FakePlayer) {
            Party party = getPartyForClaim(claim);
            return party == null || party.allowsFakePlayers();
        }

        if (player.canUseCommand(2, "")) return true;
        if (claim.ownerUUID.equals(player.getUniqueID())) return true;
        if (PartyProviderRegistry.get().areInSameParty(claim.ownerUUID, player.getUniqueID())) return true;

        return false;
    }

    // --- Priority 1: Block breaking ---

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        if (!canPlayerActAt(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    // --- Priority 1: Block placing ---

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isRemote) return;
        Entity entity = event.getEntity();
        EntityPlayer player = (entity instanceof EntityPlayer) ? (EntityPlayer) entity : null;
        if (!canPlayerActAt(player, event.getPos())) {
            event.setCanceled(true);
        }
    }

    // --- Priority 1: Right-click interactions ---

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;
        if (!canPlayerActAt(event.getEntityPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    // --- Priority 1: Explosions (per-party setting) ---

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) return;
        if (!ModConfig.enableProtection) return;

        List<BlockPos> affectedBlocks = event.getAffectedBlocks();
        affectedBlocks.removeIf(pos -> {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            ClaimedChunkData claim = ChunkManagerData.getInstance().getClaim(chunkX, chunkZ);
            if (claim == null) return false;
            Party party = getPartyForClaim(claim);
            return party == null || party.protectsExplosions();
        });

        Iterator<Entity> entityIt = event.getAffectedEntities().iterator();
        while (entityIt.hasNext()) {
            Entity entity = entityIt.next();
            int chunkX = entity.chunkCoordX;
            int chunkZ = entity.chunkCoordZ;
            ClaimedChunkData claim = ChunkManagerData.getInstance().getClaim(chunkX, chunkZ);
            if (claim == null) continue;
            Party party = getPartyForClaim(claim);
            if (party == null || party.protectsExplosions()) {
                entityIt.remove();
            }
        }
    }

    // --- Priority 2: Entity interactions ---

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote) return;
        if (!canPlayerActAt(event.getEntityPlayer(), event.getTarget().getPosition())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getWorld().isRemote) return;
        if (!canPlayerActAt(event.getEntityPlayer(), event.getTarget().getPosition())) {
            event.setCanceled(true);
        }
    }

    // --- Priority 2: Entity attack ---

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntityPlayer().world.isRemote) return;
        Entity target = event.getTarget();
        if (!canPlayerActAt(event.getEntityPlayer(), target.getPosition())) {
            event.setCanceled(true);
        }
    }

    // --- Priority 2: Mob griefing ---

    @SubscribeEvent
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (!ModConfig.enableProtection || !ModConfig.protectMobGriefing) return;
        Entity entity = event.getEntity();
        if (entity.world.isRemote) return;

        int chunkX = entity.chunkCoordX;
        int chunkZ = entity.chunkCoordZ;
        if (isChunkClaimed(chunkX, chunkZ)) {
            event.setResult(Event.Result.DENY);
        }
    }

    // --- Priority 2: Farmland trampling ---

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getWorld().isRemote) return;
        if (!ModConfig.enableProtection) return;

        Entity entity = event.getEntity();
        EntityPlayer player = (entity instanceof EntityPlayer) ? (EntityPlayer) entity : null;

        if (player != null) {
            if (!canPlayerActAt(player, event.getPos())) {
                event.setCanceled(true);
            }
        } else {
            int chunkX = event.getPos().getX() >> 4;
            int chunkZ = event.getPos().getZ() >> 4;
            if (isChunkClaimed(chunkX, chunkZ)) {
                event.setCanceled(true);
            }
        }
    }

    // --- Priority 3: Fluid block generation across chunk boundaries ---

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getWorld().isRemote) return;
        if (!ModConfig.enableProtection || !ModConfig.protectFluidFlow) return;

        BlockPos targetPos = event.getPos();
        BlockPos liquidPos = event.getLiquidPos();

        int targetChunkX = targetPos.getX() >> 4;
        int targetChunkZ = targetPos.getZ() >> 4;
        int sourceChunkX = liquidPos.getX() >> 4;
        int sourceChunkZ = liquidPos.getZ() >> 4;

        boolean targetClaimed = isChunkClaimed(targetChunkX, targetChunkZ);
        boolean sourceClaimed = isChunkClaimed(sourceChunkX, sourceChunkZ);

        if (targetClaimed && !sourceClaimed) {
            event.setCanceled(true);
        }
    }

    // --- Priority 3: Fire spread across chunk boundaries ---

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getWorld().isRemote) return;
        if (!ModConfig.enableProtection || !ModConfig.protectFireSpread) return;

        IBlockState state = event.getState();
        if (state.getBlock() != Blocks.FIRE) return;

        BlockPos firePos = event.getPos();
        int fireChunkX = firePos.getX() >> 4;
        int fireChunkZ = firePos.getZ() >> 4;

        if (!isChunkClaimed(fireChunkX, fireChunkZ)) {
            for (EnumFacing side : event.getNotifiedSides()) {
                BlockPos neighbor = firePos.offset(side);
                int nChunkX = neighbor.getX() >> 4;
                int nChunkZ = neighbor.getZ() >> 4;
                if (isChunkClaimed(nChunkX, nChunkZ)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
