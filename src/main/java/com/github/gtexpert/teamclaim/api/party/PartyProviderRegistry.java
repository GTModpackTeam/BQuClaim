package com.github.gtexpert.teamclaim.api.party;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

public class PartyProviderRegistry {

    private static final IPartyProvider NO_OP = new IPartyProvider() {

        @Override
        public boolean areInSameParty(UUID playerA, UUID playerB) {
            return false;
        }

        @Override
        @Nullable
        public String getPartyName(UUID playerUUID) {
            return null;
        }

        @Override
        public int getPartyId(UUID playerUUID) {
            return -1;
        }

        @Override
        public List<UUID> getPartyMembers(UUID playerUUID) {
            return Collections.emptyList();
        }

        @Override
        @Nullable
        public String getRole(UUID playerUUID) {
            return null;
        }

        @Override
        public boolean createParty(EntityPlayerMP player, String name) {
            return false;
        }

        @Override
        public boolean disbandParty(EntityPlayerMP player, int partyId) {
            return false;
        }

        @Override
        public boolean renameParty(EntityPlayerMP player, int partyId, String newName) {
            return false;
        }

        @Override
        public boolean invitePlayer(EntityPlayerMP inviter, int partyId, String targetUsername) {
            return false;
        }

        @Override
        public boolean acceptInvite(EntityPlayerMP player, int partyId) {
            return false;
        }

        @Override
        public boolean kickOrLeave(EntityPlayerMP actor, int partyId, String targetUsername) {
            return false;
        }

        @Override
        public boolean changeRole(EntityPlayerMP actor, int partyId, String targetUsername, String newRole) {
            return false;
        }

        @Override
        public void syncToAll() {}

        @Override
        public net.minecraft.nbt.NBTTagCompound serializeForClient() {
            return new net.minecraft.nbt.NBTTagCompound();
        }
    };

    private static IPartyProvider provider = NO_OP;
    private static Runnable nativePartyScreenOpener;

    public static void register(IPartyProvider teamProvider) {
        provider = teamProvider;
    }

    public static void registerNativeScreenOpener(Runnable opener) {
        nativePartyScreenOpener = opener;
    }

    public static IPartyProvider get() {
        return provider;
    }

    public static boolean hasNativeScreen() {
        return nativePartyScreenOpener != null;
    }

    public static void openNativeScreen() {
        if (nativePartyScreenOpener != null) {
            nativePartyScreenOpener.run();
        }
    }
}
