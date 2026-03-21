package com.github.gtexpert.teamclaim.common.party;

import java.util.*;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class ClientPartyCache {

    private static final Map<Integer, Party> teams = new TreeMap<>();
    private static final Set<UUID> bquLinkedPlayers = new HashSet<>();
    private static Runnable onSyncCallback;

    public static void setOnSyncCallback(Runnable callback) {
        onSyncCallback = callback;
    }

    public static void clearOnSyncCallback() {
        onSyncCallback = null;
    }

    public static void loadFromNBT(NBTTagCompound data) {
        teams.clear();
        bquLinkedPlayers.clear();
        NBTTagList list = data.getTagList("teams", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            Party party = Party.fromNBT(list.getCompoundTagAt(i));
            teams.put(party.getPartyId(), party);
        }
        NBTTagList linkedList = data.getTagList("bquLinked", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < linkedList.tagCount(); i++) {
            bquLinkedPlayers.add(linkedList.getCompoundTagAt(i).getUniqueId("uuid"));
        }
        if (onSyncCallback != null) {
            Runnable cb = onSyncCallback;
            onSyncCallback = null;
            cb.run();
        }
    }

    public static boolean isBQuLinked(UUID playerUUID) {
        return bquLinkedPlayers.contains(playerUUID);
    }

    public static void clear() {
        teams.clear();
    }

    @Nullable
    public static Party getParty(int partyId) {
        return teams.get(partyId);
    }

    @Nullable
    public static Party getPartyByPlayer(UUID playerUUID) {
        for (Party party : teams.values()) {
            if (party.isMember(playerUUID)) {
                return party;
            }
        }
        return null;
    }

    public static Collection<Party> getAllParties() {
        return Collections.unmodifiableCollection(teams.values());
    }
}
