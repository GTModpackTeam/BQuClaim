package com.github.gtexpert.teamclaim.integration.bqu;

import java.util.UUID;

import net.minecraft.world.World;

import com.github.gtexpert.teamclaim.TeamClaimMod;
import com.github.gtexpert.teamclaim.common.party.Party;
import com.github.gtexpert.teamclaim.common.party.PartyManagerData;
import com.github.gtexpert.teamclaim.common.party.PartyRole;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.enums.EnumPartyStatus;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.party.IParty;
import betterquesting.api2.storage.DBEntry;

public class BQMigrationHelper {

    public static void migrateIfNeeded(World world) {
        PartyManagerData data = PartyManagerData.get(world);

        if (data.isMigrated()) return;

        try {
            int count = 0;
            for (DBEntry<IParty> entry : QuestingAPI.getAPI(ApiReference.PARTY_DB).getEntries()) {
                IParty bqParty = entry.getValue();
                String name = bqParty.getProperties().getProperty(NativeProps.NAME);
                if (name == null || name.isEmpty()) name = "New Party";

                UUID ownerUUID = null;
                for (UUID memberId : bqParty.getMembers()) {
                    if (bqParty.getStatus(memberId) == EnumPartyStatus.OWNER) {
                        ownerUUID = memberId;
                        break;
                    }
                }

                if (ownerUUID == null) {
                    TeamClaimMod.LOGGER.warn("[Migration] Skipping BQu party '{}' (no owner)", name);
                    continue;
                }

                if (data.getPartyByPlayer(ownerUUID) != null) {
                    TeamClaimMod.LOGGER.debug("[Migration] Skipping BQu party '{}' (owner already in a party)", name);
                    continue;
                }

                Party party = data.createParty(name, ownerUUID);

                for (UUID memberId : bqParty.getMembers()) {
                    if (memberId.equals(ownerUUID)) continue;
                    EnumPartyStatus status = bqParty.getStatus(memberId);
                    PartyRole role = mapRole(status);
                    party.addMember(memberId, role);
                }

                count++;
                TeamClaimMod.LOGGER.info("[Migration] BQu party '{}' -> TeamClaim party (id={})", name,
                        party.getPartyId());
            }

            data.setMigrated(true);
            TeamClaimMod.LOGGER.info("[Migration] Complete. {} BQu parties migrated.", count);
        } catch (Exception e) {
            TeamClaimMod.LOGGER.error("[Migration] Failed to migrate BQu parties", e);
        }
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
