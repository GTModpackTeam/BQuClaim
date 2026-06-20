package com.github.gtexpert.blpc.api.event;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Public Forge events fired around party lifecycle mutations — addons may subscribe
 * on {@code MinecraftForge.EVENT_BUS}. {@code Pre} variants are {@link Cancelable}
 * (veto the action); {@code Post} variants are informational (mutation already occurred).
 *
 * <h3>Event tree</h3>
 * 
 * <pre>
 * PartyEvent
 * ├── Pre  (cancelable, fired before mutation)
 * │   ├── Pre.Created   – party about to be created (no partyId yet)
 * │   └── Pre.Disbanded – party about to be disbanded
 * └── Post (informational, fired after successful mutation)
 *     ├── Post.Created
 *     ├── Post.Disbanded
 *     ├── Post.MemberJoined
 *     ├── Post.MemberLeft
 *     └── Post.RoleChanged
 * </pre>
 *
 * <h3>Example usage</h3>
 * 
 * <pre>
 * {@code
 * &#64;SubscribeEvent
 * public void onPartyCreated(PartyEvent.Post.Created e) {
 *     LOGGER.info("Party '{}' created by {}", e.getPartyName(), e.getOwnerUUID());
 * }
 *
 * &#64;SubscribeEvent
 * public void onBeforeDisband(PartyEvent.Pre.Disbanded e) {
 *     if (isCriticalParty(e.getPartyId())) e.setCanceled(true);
 * }
 * }
 * </pre>
 */
public abstract class PartyEvent extends Event {

    /**
     * The party UUID.
     * <p>
     * <b>Note:</b> {@code Pre.Created} fires before the party is persisted, so its
     * {@code partyId} is {@code null}. All other events carry a non-null id.
     */
    @Nullable
    private final UUID partyId;
    private final String partyName;

    protected PartyEvent(@Nullable UUID partyId, String partyName) {
        this.partyId = partyId;
        this.partyName = partyName;
    }

    @Nullable
    public UUID getPartyId() {
        return partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    // -------------------------------------------------------------------------
    // Pre (cancelable) events
    // -------------------------------------------------------------------------

    public abstract static class Pre extends PartyEvent {

        protected Pre(@Nullable UUID partyId, String partyName) {
            super(partyId, partyName);
        }

        /**
         * Fired before a new party is created.
         * <p>
         * {@link #getPartyId()} returns {@code null} because the party does not
         * exist yet; cancel this event to prevent creation.
         */
        @Cancelable
        public static class Created extends Pre {

            private final UUID ownerUUID;

            public Created(String partyName, UUID ownerUUID) {
                super(null, partyName);
                this.ownerUUID = ownerUUID;
            }

            public UUID getOwnerUUID() {
                return ownerUUID;
            }
        }

        /** Fired before a party is disbanded; cancel to prevent it. */
        @Cancelable
        public static class Disbanded extends Pre {

            public Disbanded(UUID partyId, String partyName) {
                super(partyId, partyName);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Post (informational) events
    // -------------------------------------------------------------------------

    public abstract static class Post extends PartyEvent {

        protected Post(UUID partyId, String partyName) {
            super(partyId, partyName);
        }

        /** Fired after a party is successfully created. */
        public static class Created extends Post {

            private final UUID ownerUUID;

            public Created(UUID partyId, String partyName, UUID ownerUUID) {
                super(partyId, partyName);
                this.ownerUUID = ownerUUID;
            }

            public UUID getOwnerUUID() {
                return ownerUUID;
            }
        }

        /** Fired after a party is successfully disbanded. */
        public static class Disbanded extends Post {

            private final List<UUID> memberUUIDs;

            public Disbanded(UUID partyId, String partyName, List<UUID> memberUUIDs) {
                super(partyId, partyName);
                this.memberUUIDs = Collections.unmodifiableList(memberUUIDs);
            }

            /** UUIDs of all members who were in the party at the time of disbanding. */
            public List<UUID> getMemberUUIDs() {
                return memberUUIDs;
            }
        }

        /** Fired after a player successfully joins a party (invite accepted or free-join). */
        public static class MemberJoined extends Post {

            private final UUID memberUUID;

            public MemberJoined(UUID partyId, String partyName, UUID memberUUID) {
                super(partyId, partyName);
                this.memberUUID = memberUUID;
            }

            public UUID getMemberUUID() {
                return memberUUID;
            }
        }

        /** Fired after a player leaves or is kicked from a party. */
        public static class MemberLeft extends Post {

            private final UUID memberUUID;
            private final boolean wasKicked;

            public MemberLeft(UUID partyId, String partyName, UUID memberUUID, boolean wasKicked) {
                super(partyId, partyName);
                this.memberUUID = memberUUID;
                this.wasKicked = wasKicked;
            }

            public UUID getMemberUUID() {
                return memberUUID;
            }

            /**
             * Returns {@code true} if the member was kicked by another player; {@code false} if they left voluntarily.
             */
            public boolean wasKicked() {
                return wasKicked;
            }
        }

        /** Fired after a party member's role is successfully changed. */
        public static class RoleChanged extends Post {

            private final UUID playerUUID;
            private final String oldRole;
            private final String newRole;

            public RoleChanged(UUID partyId, String partyName, UUID playerUUID, String oldRole, String newRole) {
                super(partyId, partyName);
                this.playerUUID = playerUUID;
                this.oldRole = oldRole;
                this.newRole = newRole;
            }

            public UUID getPlayerUUID() {
                return playerUUID;
            }

            public String getOldRole() {
                return oldRole;
            }

            public String getNewRole() {
                return newRole;
            }
        }
    }
}
