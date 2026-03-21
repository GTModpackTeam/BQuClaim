package com.github.gtexpert.teamclaim.integration.bqu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;

import com.github.gtexpert.teamclaim.TeamClaimMod;
import com.github.gtexpert.teamclaim.api.party.IPartyProvider;
import com.github.gtexpert.teamclaim.common.chunk.ChunkManagerData;
import com.github.gtexpert.teamclaim.common.chunk.ClaimedChunkData;
import com.github.gtexpert.teamclaim.common.chunk.TicketManager;
import com.github.gtexpert.teamclaim.common.network.MessageSyncClaims;
import com.github.gtexpert.teamclaim.common.network.MessageTeamSync;
import com.github.gtexpert.teamclaim.common.network.ModNetwork;
import com.github.gtexpert.teamclaim.common.party.DefaultPartyProvider;
import com.github.gtexpert.teamclaim.common.party.Party;
import com.github.gtexpert.teamclaim.common.party.PartyRole;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.enums.EnumPartyStatus;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.party.IParty;
import betterquesting.api2.storage.DBEntry;
import betterquesting.network.handlers.NetPartySync;
import betterquesting.questing.party.PartyInvitations;
import betterquesting.questing.party.PartyManager;

public class BQPartyProvider implements IPartyProvider {

    private final DefaultPartyProvider fallback = new DefaultPartyProvider();

    // --- Query: BQu first, fallback to self-managed ---

    @Override
    public boolean areInSameParty(UUID playerA, UUID playerB) {
        try {
            for (DBEntry<IParty> entry : QuestingAPI.getAPI(ApiReference.PARTY_DB).getEntries()) {
                IParty party = entry.getValue();
                if (party.getStatus(playerA) != null && party.getStatus(playerB) != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            TeamClaimMod.LOGGER.debug("Failed to check party status", e);
        }
        return fallback.areInSameParty(playerA, playerB);
    }

    @Override
    @Nullable
    public String getPartyName(UUID playerUUID) {
        DBEntry<IParty> entry = PartyManager.INSTANCE.getParty(playerUUID);
        if (entry != null) return entry.getValue().getProperties().getProperty(NativeProps.NAME);
        return fallback.getPartyName(playerUUID);
    }

    @Override
    public int getPartyId(UUID playerUUID) {
        DBEntry<IParty> entry = PartyManager.INSTANCE.getParty(playerUUID);
        if (entry != null) return entry.getID();
        return fallback.getPartyId(playerUUID);
    }

    @Override
    public List<UUID> getPartyMembers(UUID playerUUID) {
        DBEntry<IParty> entry = PartyManager.INSTANCE.getParty(playerUUID);
        if (entry != null) return new ArrayList<>(entry.getValue().getMembers());
        return fallback.getPartyMembers(playerUUID);
    }

    @Override
    @Nullable
    public String getRole(UUID playerUUID) {
        DBEntry<IParty> entry = PartyManager.INSTANCE.getParty(playerUUID);
        if (entry != null) {
            EnumPartyStatus status = entry.getValue().getStatus(playerUUID);
            return status != null ? status.name() : null;
        }
        return fallback.getRole(playerUUID);
    }

    @Override
    public boolean createParty(EntityPlayerMP player, String name) {
        UUID playerId = QuestingAPI.getQuestingUUID(player);
        if (PartyManager.INSTANCE.getParty(playerId) != null) return false;

        int partyId = PartyManager.INSTANCE.nextID();
        IParty party = PartyManager.INSTANCE.createNew(partyId);
        party.getProperties().setProperty(NativeProps.NAME, name);
        party.setStatus(playerId, EnumPartyStatus.OWNER);
        NetPartySync.sendSync(new EntityPlayerMP[] { player }, new int[] { partyId });
        // Mirror to self-managed data
        fallback.createParty(player, name);
        return true;
    }

    @Override
    public boolean disbandParty(EntityPlayerMP player, int partyId) {
        IParty party = PartyManager.INSTANCE.getValue(partyId);
        if (party == null) return false;

        UUID playerId = QuestingAPI.getQuestingUUID(player);
        EnumPartyStatus status = party.getStatus(playerId);
        if (status != EnumPartyStatus.OWNER && !player.canUseCommand(2, "")) return false;

        // Release all members' claims
        ChunkManagerData chunkData = ChunkManagerData.get(player.world);
        for (UUID memberId : party.getMembers()) {
            for (ClaimedChunkData claim : chunkData.getClaimsByOwner(memberId)) {
                if (claim.isForceLoaded) {
                    TicketManager.unforceChunk(player.world, claim.x, claim.z);
                }
                chunkData.setClaim(claim.x, claim.z, null, "", "", false);
                ModNetwork.INSTANCE.sendToAll(new MessageSyncClaims(claim.x, claim.z, null, "", "", false));
            }
        }

        PartyManager.INSTANCE.removeID(partyId);
        PartyInvitations.INSTANCE.purgeInvites(partyId);
        NetPartySync.sendSync(null, null);
        return true;
    }

    @Override
    public boolean renameParty(EntityPlayerMP player, int partyId, String newName) {
        IParty party = PartyManager.INSTANCE.getValue(partyId);
        if (party == null) return false;

        UUID playerId = QuestingAPI.getQuestingUUID(player);
        EnumPartyStatus status = party.getStatus(playerId);
        if (status != EnumPartyStatus.OWNER && !player.canUseCommand(2, "")) return false;

        party.getProperties().setProperty(NativeProps.NAME, newName);
        NetPartySync.quickSync(partyId);
        return true;
    }

    @Override
    public boolean invitePlayer(EntityPlayerMP inviter, int partyId, String targetUsername) {
        IParty party = PartyManager.INSTANCE.getValue(partyId);
        if (party == null) return false;

        UUID inviterId = QuestingAPI.getQuestingUUID(inviter);
        EnumPartyStatus status = party.getStatus(inviterId);
        if (status == null || status.ordinal() < EnumPartyStatus.ADMIN.ordinal()) {
            if (!inviter.canUseCommand(2, "")) return false;
        }

        MinecraftServer server = inviter.getServer();
        if (server == null) return false;
        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(targetUsername);
        if (target == null) return false;
        UUID targetId = QuestingAPI.getQuestingUUID(target);
        if (party.getStatus(targetId) != null) return false;
        if (PartyManager.INSTANCE.getParty(targetId) != null) return false;

        PartyInvitations.INSTANCE.postInvite(targetId, partyId, 300000L);
        NetPartySync.sendSync(new EntityPlayerMP[] { target }, new int[] { partyId });
        return true;
    }

    @Override
    public boolean acceptInvite(EntityPlayerMP player, int partyId) {
        UUID playerId = QuestingAPI.getQuestingUUID(player);
        if (PartyManager.INSTANCE.getParty(playerId) != null) return false;

        boolean accepted = PartyInvitations.INSTANCE.acceptInvite(playerId, partyId);
        if (accepted) {
            NetPartySync.quickSync(partyId);
            // Mirror: add member to self-managed party
            fallback.acceptInvite(player, partyId);
        }
        return accepted;
    }

    @Override
    public boolean kickOrLeave(EntityPlayerMP actor, int partyId, String targetUsername) {
        IParty party = PartyManager.INSTANCE.getValue(partyId);
        if (party == null) return false;

        MinecraftServer server = actor.getServer();
        if (server == null) return false;

        UUID actorId = QuestingAPI.getQuestingUUID(actor);

        if (targetUsername.equals(actor.getName())) {
            if (party.getStatus(actorId) == EnumPartyStatus.OWNER) return false;
            party.kickUser(actorId);
        } else {
            EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(targetUsername);
            if (target == null) return false;
            UUID targetId = QuestingAPI.getQuestingUUID(target);

            EnumPartyStatus actorStatus = party.getStatus(actorId);
            EnumPartyStatus targetStatus = party.getStatus(targetId);
            if (actorStatus == null || targetStatus == null) return false;
            if (!actor.canUseCommand(2, "") && actorStatus.ordinal() <= targetStatus.ordinal()) return false;

            party.kickUser(targetId);
        }

        NetPartySync.quickSync(partyId);
        return true;
    }

    @Override
    public boolean changeRole(EntityPlayerMP actor, int partyId, String targetUsername, String newRole) {
        IParty party = PartyManager.INSTANCE.getValue(partyId);
        if (party == null) return false;

        UUID actorId = QuestingAPI.getQuestingUUID(actor);
        EnumPartyStatus actorStatus = party.getStatus(actorId);
        if (actorStatus != EnumPartyStatus.OWNER && !actor.canUseCommand(2, "")) return false;

        MinecraftServer server = actor.getServer();
        if (server == null) return false;
        EntityPlayerMP target = server.getPlayerList().getPlayerByUsername(targetUsername);
        if (target == null) return false;
        UUID targetId = QuestingAPI.getQuestingUUID(target);
        if (party.getStatus(targetId) == null) return false;

        EnumPartyStatus role;
        try {
            role = EnumPartyStatus.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            return false;
        }

        party.setStatus(targetId, role);
        NetPartySync.quickSync(partyId);
        return true;
    }

    @Override
    public void syncToAll() {
        NetPartySync.sendSync(null, null);
        ModNetwork.INSTANCE.sendToAll(new MessageTeamSync(serializeForClient()));
    }

    @Override
    public NBTTagCompound serializeForClient() {
        // Merge BQu parties + self-managed parties (for players not in any BQu party)
        NBTTagList list = new NBTTagList();
        Set<UUID> bquMembers = new HashSet<>();

        // Add all BQu parties
        for (DBEntry<IParty> entry : PartyManager.INSTANCE.getEntries()) {
            IParty bqParty = entry.getValue();
            if (bqParty.getMembers().isEmpty()) continue;
            Party party = new Party(entry.getID(),
                    bqParty.getProperties().getProperty(NativeProps.NAME),
                    0L);
            for (UUID memberId : bqParty.getMembers()) {
                EnumPartyStatus status = bqParty.getStatus(memberId);
                party.addMember(memberId, mapRole(status));
                bquMembers.add(memberId);
            }
            list.appendTag(party.toNBT());
        }

        // Add self-managed parties for players NOT in any BQu party
        NBTTagCompound selfData = fallback.serializeForClient();
        NBTTagList selfList = selfData.getTagList("teams", 10);
        for (int i = 0; i < selfList.tagCount(); i++) {
            Party selfParty = Party.fromNBT(selfList.getCompoundTagAt(i));
            boolean hasNonBQuMember = false;
            for (UUID memberId : selfParty.getMemberUUIDs()) {
                if (!bquMembers.contains(memberId)) {
                    hasNonBQuMember = true;
                    break;
                }
            }
            if (hasNonBQuMember) {
                list.appendTag(selfParty.toNBT());
            }
        }

        NBTTagCompound root = new NBTTagCompound();
        root.setTag("teams", list);
        return root;
    }

    private static PartyRole mapRole(EnumPartyStatus status) {
        if (status == null) return PartyRole.MEMBER;
        switch (status) {
            case OWNER:
                return PartyRole.OWNER;
            case ADMIN:
                return PartyRole.ADMIN;
            default:
                return PartyRole.MEMBER;
        }
    }
}
