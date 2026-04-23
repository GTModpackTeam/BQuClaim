package com.github.gtexpert.blpc.common.chunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import com.github.gtexpert.blpc.common.network.MessageSyncClaims;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;

/**
 * Server-side chunk claim storage. Singleton, persisted by {@link com.github.gtexpert.blpc.common.BLPCSaveHandler}.
 * Claims are keyed by a packed {@code long} (x &lt;&lt; 32 | z) and grouped per-party in save files.
 */
public class ChunkManagerData {

    private static volatile ChunkManagerData instance;

    private final Map<Long, ClaimedChunkData> claims = new ConcurrentHashMap<>();
    private final Queue<ClaimedChunkData> pendingClaims = new ConcurrentLinkedQueue<>();

    public static synchronized ChunkManagerData getInstance() {
        if (instance == null) {
            instance = new ChunkManagerData();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new ChunkManagerData();
    }

    public static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public ClaimedChunkData getClaim(int x, int z) {
        return claims.get(chunkKey(x, z));
    }

    public void setClaim(int x, int z, UUID owner, String name, String partyName, boolean isForceLoaded) {
        long key = chunkKey(x, z);
        if (owner == null) {
            claims.remove(key);
        } else {
            claims.put(key, new ClaimedChunkData(x, z, owner, name, partyName, isForceLoaded));
        }
    }

    public Collection<ClaimedChunkData> getAllClaims() {
        return Collections.unmodifiableCollection(claims.values());
    }

    public List<ClaimedChunkData> getClaimsByOwner(UUID owner) {
        return claims.values().stream()
                .filter(d -> d.ownerUUID.equals(owner))
                .collect(Collectors.toList());
    }

    public int countClaims(UUID owner) {
        return (int) claims.values().stream()
                .filter(d -> d.ownerUUID.equals(owner))
                .count();
    }

    public int countForceLoads(UUID owner) {
        return (int) claims.values().stream()
                .filter(d -> d.ownerUUID.equals(owner) && d.isForceLoaded)
                .count();
    }

    public int countClaimsForParty(UUID partyId) {
        Party party = PartyManagerData.getInstance().getParty(partyId);
        if (party == null) return 0;
        Set<UUID> memberIds = new HashSet<>(party.getMemberUUIDs());
        return (int) claims.values().stream()
                .filter(d -> memberIds.contains(d.ownerUUID))
                .count();
    }

    public int countForceLoadsForParty(UUID partyId) {
        Party party = PartyManagerData.getInstance().getParty(partyId);
        if (party == null) return 0;
        Set<UUID> memberIds = new HashSet<>(party.getMemberUUIDs());
        return (int) claims.values().stream()
                .filter(d -> memberIds.contains(d.ownerUUID) && d.isForceLoaded)
                .count();
    }

    public void enqueueClaim(ClaimedChunkData data) {
        pendingClaims.add(data);
    }

    public void flushPending() {
        ClaimedChunkData d;
        while ((d = pendingClaims.poll()) != null) {
            claims.put(chunkKey(d.x, d.z), d);
        }
    }

    /**
     * Removes all claims and force-loads for the given player, broadcasting unclaim messages.
     * <p>
     * Iterates all loaded worlds to release force-load tickets, since claims are stored
     * globally but tickets are keyed per-dimension.
     */
    public void releaseAllClaims(UUID owner, World world) {
        for (ClaimedChunkData claim : getClaimsByOwner(owner)) {
            if (claim.isForceLoaded) {
                // Unforce across all loaded dimensions to avoid ticket leaks
                for (WorldServer ws : FMLCommonHandler.instance().getMinecraftServerInstance().worlds) {
                    TicketManager.unforceChunk(ws, claim.x, claim.z);
                }
            }
            setClaim(claim.x, claim.z, null, "", "", false);
            ModNetwork.INSTANCE.sendToAll(new MessageSyncClaims(claim.x, claim.z, null, "", "", false));
        }
    }

    /**
     * Transfers all chunk claims from {@code oldOwner} to {@code newOwner}.
     * Used when merging offline/online UUIDs on player login.
     */
    public void transferOwnership(UUID oldOwner, UUID newOwner) {
        for (ClaimedChunkData claim : getClaimsByOwner(oldOwner)) {
            claims.put(
                    chunkKey(claim.x, claim.z),
                    new ClaimedChunkData(claim.x, claim.z, newOwner, claim.ownerName, claim.partyName,
                            claim.isForceLoaded));
        }
    }

    public NBTTagCompound serializeAll() {
        NBTTagCompound all = new NBTTagCompound();
        for (Map.Entry<Long, ClaimedChunkData> entry : claims.entrySet()) {
            all.setTag(Long.toString(entry.getKey()), entry.getValue().toNBT());
        }
        return all;
    }
}
