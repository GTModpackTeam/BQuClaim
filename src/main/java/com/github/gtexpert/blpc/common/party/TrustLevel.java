package com.github.gtexpert.blpc.common.party;

/**
 * Represents a player's trust level within a party's claimed chunks.
 * <p>
 * Trust levels are ordered by ascending privilege. Each {@link TrustAction}
 * has a configurable required level; a player can perform the action only if
 * their effective level is at least the required level (see {@link #isAtLeast}).
 * <p>
 * Ordering: {@code NONE < ALLY < MEMBER < MODERATOR < OWNER}.
 */
public enum TrustLevel {

    /** No trust -- outsiders with no relationship to the party. */
    NONE,
    /** Allied player -- explicitly added to the party's ally list. */
    ALLY,
    /** Party member -- a regular member of the party. */
    MEMBER,
    /** Moderator -- maps from {@link PartyRole#ADMIN}. */
    MODERATOR,
    /** Owner -- the party creator / current owner. */
    OWNER;

    public boolean isAtLeast(TrustLevel required) {
        return this.ordinal() >= required.ordinal();
    }

    public static TrustLevel fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
