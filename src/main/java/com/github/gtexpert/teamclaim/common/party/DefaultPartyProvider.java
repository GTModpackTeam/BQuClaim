package com.github.gtexpert.teamclaim.common.party;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import com.github.gtexpert.teamclaim.api.party.IPartyProvider;
import com.github.gtexpert.teamclaim.common.chunk.ChunkManagerData;
import com.github.gtexpert.teamclaim.common.chunk.ClaimedChunkData;
import com.github.gtexpert.teamclaim.common.chunk.TicketManager;
import com.github.gtexpert.teamclaim.common.network.MessageSyncClaims;
import com.github.gtexpert.teamclaim.common.network.MessageTeamSync;
import com.github.gtexpert.teamclaim.common.network.ModNetwork;

public class DefaultPartyProvider implements IPartyProvider {

    @Override
    public boolean areInSameParty(UUID playerA, UUID playerB) {
        PartyManagerData data = getPartyData();
        if (data == null) return false;
        Party party = data.getPartyByPlayer(playerA);
        return party != null && party.isMember(playerB);
    }

    @Override
    @Nullable
    public String getPartyName(UUID playerUUID) {
        PartyManagerData data = getPartyData();
        if (data == null) return null;
        Party party = data.getPartyByPlayer(playerUUID);
        return party != null ? party.getName() : null;
    }

    @Override
    public int getPartyId(UUID playerUUID) {
        PartyManagerData data = getPartyData();
        if (data == null) return -1;
        Party party = data.getPartyByPlayer(playerUUID);
        return party != null ? party.getPartyId() : -1;
    }

    @Override
    public List<UUID> getPartyMembers(UUID playerUUID) {
        PartyManagerData data = getPartyData();
        if (data == null) return Collections.emptyList();
        Party party = data.getPartyByPlayer(playerUUID);
        return party != null ? party.getMemberUUIDs() : Collections.emptyList();
    }

    @Override
    @Nullable
    public String getRole(UUID playerUUID) {
        PartyManagerData data = getPartyData();
        if (data == null) return null;
        Party party = data.getPartyByPlayer(playerUUID);
        if (party == null) return null;
        PartyRole role = party.getRole(playerUUID);
        return role != null ? role.name() : null;
    }

    @Override
    public boolean createParty(EntityPlayerMP player, String name) {
        PartyManagerData data = PartyManagerData.get(player.world);
        UUID playerId = player.getUniqueID();
        if (data.getPartyByPlayer(playerId) != null) return false;
        data.createParty(name, playerId);
        return true;
    }

    @Override
    public boolean disbandParty(EntityPlayerMP player, int partyId) {
        PartyManagerData data = PartyManagerData.get(player.world);
        Party party = data.getParty(partyId);
        if (party == null) return false;

        // Release all members' claims
        ChunkManagerData chunkData = ChunkManagerData.get(player.world);
        for (UUID memberId : party.getMemberUUIDs()) {
            for (ClaimedChunkData claim : chunkData.getClaimsByOwner(memberId)) {
                if (claim.isForceLoaded) {
                    TicketManager.unforceChunk(player.world, claim.x, claim.z);
                }
                chunkData.setClaim(claim.x, claim.z, null, "", "", false);
                ModNetwork.INSTANCE.sendToAll(new MessageSyncClaims(claim.x, claim.z, null, "", "", false));
            }
        }

        data.removeParty(partyId);
        return true;
    }

    @Override
    public boolean renameParty(EntityPlayerMP player, int partyId, String newName) {
        PartyManagerData data = PartyManagerData.get(player.world);
        Party party = data.getParty(partyId);
        if (party == null) return false;
        party.setName(newName);
        return true;
    }

    @Override
    public boolean invitePlayer(EntityPlayerMP inviter, int partyId, String targetUsername) {
        PartyManagerData data = PartyManagerData.get(inviter.world);
        Party party = data.getParty(partyId);
        if (party == null) return false;
        MinecraftServer server = inviter.getServer();
        if (server == null) return false;
        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(targetUsername);
        if (target == null) return false;
        UUID targetId = target.getUniqueID();
        if (party.isMember(targetId)) return false;
        if (data.getPartyByPlayer(targetId) != null) return false;
        party.addInvite(targetId, System.currentTimeMillis() + 300000L);
        return true;
    }

    @Override
    public boolean acceptInvite(EntityPlayerMP player, int partyId) {
        PartyManagerData data = PartyManagerData.get(player.world);
        UUID playerId = player.getUniqueID();
        if (data.getPartyByPlayer(playerId) != null) return false;
        Party party = data.getParty(partyId);
        if (party == null) return false;
        if (!party.hasInvite(playerId)) return false;
        party.removeInvite(playerId);
        party.addMember(playerId, PartyRole.MEMBER);
        return true;
    }

    @Override
    public boolean kickOrLeave(EntityPlayerMP actor, int partyId, String targetUsername) {
        PartyManagerData data = PartyManagerData.get(actor.world);
        Party party = data.getParty(partyId);
        if (party == null) return false;
        MinecraftServer server = actor.getServer();
        if (server == null) return false;

        UUID targetId;
        if (targetUsername.equals(actor.getName())) {
            targetId = actor.getUniqueID();
            if (party.getRole(targetId) == PartyRole.OWNER) return false;
        } else {
            EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(targetUsername);
            if (target == null) return false;
            targetId = target.getUniqueID();
            PartyRole actorRole = party.getRole(actor.getUniqueID());
            PartyRole targetRole = party.getRole(targetId);
            if (actorRole == null || targetRole == null) return false;
            if (!actorRole.canKick(targetRole)) return false;
        }

        party.removeMember(targetId);
        return true;
    }

    @Override
    public boolean changeRole(EntityPlayerMP actor, int partyId, String targetUsername, String newRole) {
        PartyManagerData data = PartyManagerData.get(actor.world);
        Party party = data.getParty(partyId);
        if (party == null) return false;
        MinecraftServer server = actor.getServer();
        if (server == null) return false;
        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(targetUsername);
        if (target == null) return false;
        if (!party.isMember(target.getUniqueID())) return false;
        PartyRole role;
        try {
            role = PartyRole.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            return false;
        }
        party.setRole(target.getUniqueID(), role);
        return true;
    }

    @Override
    public void syncToAll() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        PartyManagerData data = PartyManagerData.get(server.getEntityWorld());
        ModNetwork.INSTANCE.sendToAll(new MessageTeamSync(data.serializeAll()));
    }

    @Override
    public net.minecraft.nbt.NBTTagCompound serializeForClient() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return new net.minecraft.nbt.NBTTagCompound();
        return PartyManagerData.get(server.getEntityWorld()).serializeAll();
    }

    @Nullable
    private static PartyManagerData getPartyData() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return null;
        return PartyManagerData.get(server.getEntityWorld());
    }
}
