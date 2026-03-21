package com.github.gtexpert.teamclaim.integration.bqu;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.teamclaim.common.party.ClientPartyCache;
import com.github.gtexpert.teamclaim.common.party.Party;
import com.github.gtexpert.teamclaim.common.party.PartyRole;

import betterquesting.api.enums.EnumPartyStatus;
import betterquesting.api.events.DatabaseEvent;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.party.IParty;
import betterquesting.api2.storage.DBEntry;
import betterquesting.questing.party.PartyManager;

public class BQPartyEventHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPartyUpdate(DatabaseEvent.Update event) {
        if (event.getType() != DatabaseEvent.DBType.PARTY && event.getType() != DatabaseEvent.DBType.ALL) return;

        // Build party list from BQu
        NBTTagList list = new NBTTagList();
        Set<UUID> bquMembers = new HashSet<>();
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

        // Also keep self-managed parties for players NOT in any BQu party.
        // This preserves chunk claim associations when BQu party is deleted.
        for (Party existing : ClientPartyCache.getAllParties()) {
            boolean allInBQu = true;
            for (UUID memberId : existing.getMemberUUIDs()) {
                if (!bquMembers.contains(memberId)) {
                    allInBQu = false;
                    break;
                }
            }
            if (!allInBQu) {
                list.appendTag(existing.toNBT());
            }
        }

        NBTTagCompound root = new NBTTagCompound();
        root.setTag("teams", list);
        ClientPartyCache.loadFromNBT(root);
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
