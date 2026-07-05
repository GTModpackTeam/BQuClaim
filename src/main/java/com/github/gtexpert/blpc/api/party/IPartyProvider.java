package com.github.gtexpert.blpc.api.party;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Service Provider Interface for party management.
 * <p>
 * Implementations handle both query and mutation operations.
 * All mutation methods identify the player's party by their UUID,
 * eliminating the need for explicit party ID parameters.
 * <p>
 * Two implementations exist:
 * <ul>
 * <li>{@code DefaultPartyProvider} — self-managed via {@code PartyManagerData}</li>
 * <li>{@code BQuPartyProvider} — delegates to BetterQuesting's party system with self-managed fallback</li>
 * </ul>
 */
public interface IPartyProvider {

    // --- Query ---

    /** Checks if two players belong to the same party. */
    boolean areInSameParty(UUID playerA, UUID playerB);

    /** Returns the party name for the player, or null if not in a party. */
    @Nullable
    String getPartyName(UUID playerUUID);

    /** Returns member UUIDs of the player's party, or empty if no party. */
    List<UUID> getPartyMembers(UUID playerUUID);

    /** Returns the player's role name (e.g. "OWNER","ADMIN","MEMBER"), or null. */
    @Nullable
    String getRole(UUID playerUUID);

    /** Returns the party with the given name, or null if none exists. */
    @Nullable
    default Party findByName(String name) {
        return null;
    }

    /** Returns the names of all known parties. */
    default List<String> allPartyNames() {
        return Collections.emptyList();
    }

    /** Returns all parties that have a pending invite for the given player. */
    default List<Party> pendingInvitesFor(UUID playerUUID) {
        return Collections.emptyList();
    }

    // --- Mutation (player UUID identifies the party) ---

    /** Creates a new party with the player as OWNER. Returns false if player already has a party. */
    boolean createParty(EntityPlayerMP player, String name);

    /** Disbands the player's party. Releases all members' chunk claims. */
    boolean disbandParty(EntityPlayerMP player);

    /** Renames the player's party. Requires OWNER role. */
    boolean renameParty(EntityPlayerMP player, String newName);

    /** Invites a player by username. Requires ADMIN+ role. */
    boolean invitePlayer(EntityPlayerMP inviter, String targetUsername);

    /** Accepts a pending invite to the specified party. */
    boolean acceptInvite(EntityPlayerMP player, UUID partyId);

    /** Kicks a player or leaves the party. Owner cannot leave without transferring ownership. */
    boolean kickOrLeave(EntityPlayerMP actor, String targetUsername);

    /** Changes a member's role. Requires OWNER role. */
    boolean changeRole(EntityPlayerMP actor, String targetUsername, String newRole);

    /** Returns true if the player is in a native (non-fallback) party managed by this provider. */
    default boolean hasNativeParty(UUID playerUUID) {
        return getPartyName(playerUUID) != null;
    }

    /**
     * Ensures a native party exists for the owner with the same members as the
     * given BLPC party. Creates the native party if absent, adds missing
     * members, and maps roles. Returns {@code true} if the native party is
     * ready for linking after this call.
     */
    default boolean ensureNativePartyWithMembers(EntityPlayerMP owner,
                                                 Party blpcParty) {
        return hasNativeParty(owner.getUniqueID());
    }

    /** Syncs party data to all connected clients after mutations. */
    void syncToAll();

    /**
     * Sends the current authoritative party snapshot to a single player.
     * Used to roll back optimistic client mutations when an action is rejected
     * server-side — broadcasting via {@link #syncToAll} would be wasteful when
     * only the actor's local cache is divergent.
     * <p>
     * Implementations that support per-player sync should override this.
     */
    default void syncToPlayer(EntityPlayerMP player) {}

    /** Returns NBT data representing all parties for client-side cache. */
    NBTTagCompound serializeForClient();
}
