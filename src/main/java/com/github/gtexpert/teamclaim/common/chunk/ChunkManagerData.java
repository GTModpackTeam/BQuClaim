package com.github.gtexpert.teamclaim.common.chunk;

import java.util.*;
import java.util.stream.Collectors;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

public class ChunkManagerData {

    private static ChunkManagerData instance;

    private final Map<String, ClaimedChunkData> claims = new HashMap<>();

    public static ChunkManagerData get(World world) {
        if (instance == null) {
            instance = new ChunkManagerData();
        }
        return instance;
    }

    public static ChunkManagerData getInstance() {
        if (instance == null) {
            instance = new ChunkManagerData();
        }
        return instance;
    }

    public static void reset() {
        instance = new ChunkManagerData();
    }

    public static String chunkKey(int x, int z) {
        return x + "," + z;
    }

    public ClaimedChunkData getClaim(int x, int z) {
        return claims.get(chunkKey(x, z));
    }

    public void setClaim(int x, int z, UUID owner, String name, String partyName, boolean isForceLoaded) {
        String key = chunkKey(x, z);
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
        int count = 0;
        for (ClaimedChunkData d : claims.values()) {
            if (d.ownerUUID.equals(owner)) count++;
        }
        return count;
    }

    public int countForceLoads(UUID owner) {
        int count = 0;
        for (ClaimedChunkData d : claims.values()) {
            if (d.ownerUUID.equals(owner) && d.isForceLoaded) count++;
        }
        return count;
    }

    public NBTTagCompound serializeAll() {
        NBTTagCompound all = new NBTTagCompound();
        for (Map.Entry<String, ClaimedChunkData> entry : claims.entrySet()) {
            all.setTag(entry.getKey(), entry.getValue().toNBT());
        }
        return all;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList list = nbt.getTagList("claims", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            ClaimedChunkData d = ClaimedChunkData.fromNBT(list.getCompoundTagAt(i));
            claims.put(chunkKey(d.x, d.z), d);
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (ClaimedChunkData d : claims.values()) {
            list.appendTag(d.toNBT());
        }
        nbt.setTag("claims", list);
        return nbt;
    }
}
