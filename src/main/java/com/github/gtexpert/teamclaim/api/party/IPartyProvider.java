package com.github.gtexpert.teamclaim.api.party;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

public interface IPartyProvider {

    // --- Query ---

    boolean areInSameParty(UUID playerA, UUID playerB);

    @Nullable
    String getPartyName(UUID playerUUID);

    /** Returns the team ID the player belongs to, or -1 if none. */
    int getPartyId(UUID playerUUID);

    /** Returns member UUIDs of the player's team, or empty if no team. */
    List<UUID> getPartyMembers(UUID playerUUID);

    /** Returns the player's role name (e.g. "OWNER","ADMIN","MEMBER"), or null. */
    @Nullable
    String getRole(UUID playerUUID);

    // --- Mutation ---

    boolean createParty(EntityPlayerMP player, String name);

    boolean disbandParty(EntityPlayerMP player, int partyId);

    boolean renameParty(EntityPlayerMP player, int partyId, String newName);

    boolean invitePlayer(EntityPlayerMP inviter, int partyId, String targetUsername);

    boolean acceptInvite(EntityPlayerMP player, int partyId);

    boolean kickOrLeave(EntityPlayerMP actor, int partyId, String targetUsername);

    boolean changeRole(EntityPlayerMP actor, int partyId, String targetUsername, String newRole);

    /** Called after mutations to sync data to clients. */
    void syncToAll();

    /** Returns NBT data for client-side party cache sync. */
    net.minecraft.nbt.NBTTagCompound serializeForClient();
}
