package com.github.gtexpert.blpc.common.command;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.api.util.PartyQueryUtil;
import com.github.gtexpert.blpc.common.party.DefaultPartyProvider;
import com.github.gtexpert.blpc.common.party.PartyManagerData;

/**
 * Internal command-layer helpers. Query methods delegate to {@link PartyQueryUtil};
 * only {@link #activeProviderFor} stays here because it depends on the BQu-link flag
 * stored in {@link PartyManagerData}.
 */
public final class BLPCCommandHelper {

    private static final DefaultPartyProvider SELF_PROVIDER = new DefaultPartyProvider();

    private BLPCCommandHelper() {}

    @Nullable
    public static Party findPartyByName(String name) {
        return PartyQueryUtil.findByName(name);
    }

    /** Like {@link #findPartyByName}, but throws the standard "not found" error instead of returning null. */
    public static Party requirePartyByName(String name) throws CommandException {
        Party party = findPartyByName(name);
        if (party == null) {
            throw new CommandException("Party not found: " + name);
        }
        return party;
    }

    public static List<String> allPartyNames() {
        return PartyQueryUtil.allPartyNames();
    }

    /** Returns parties that have a pending invite for the given player. */
    public static List<Party> pendingInvitesFor(UUID playerUUID) {
        return PartyQueryUtil.pendingInvitesFor(playerUUID);
    }

    /** Resolves a display name for a UUID: online player → cached party name → UsernameCache → UUID prefix. */
    public static String resolveName(MinecraftServer server, Party party, UUID uuid) {
        return PartyQueryUtil.resolveName(server, party, uuid);
    }

    /** Resolves the party owner's display name, or {@code "-"} when the party has no owner. */
    public static String resolveOwnerName(MinecraftServer server, Party party) {
        UUID owner = party.getOwner();
        return owner != null ? resolveName(server, party, owner) : "-";
    }

    /**
     * Returns the provider that should handle a player-initiated mutation.
     * BQu-linked players use the registered BQu provider; others use the self-managed default.
     */
    public static IPartyProvider activeProviderFor(EntityPlayerMP player) {
        boolean linked = PartyManagerData.getInstance().isBQuLinked(player.getUniqueID());
        return linked ? PartyProviderRegistry.get() : SELF_PROVIDER;
    }
}
