package com.github.gtexpert.blpc.api.party;

import com.github.gtexpert.blpc.api.util.EnumUtils;

/**
 * Relationship between a player and a chunk-owning party.
 */
public enum RelationType {

    MEMBER,
    ALLY,
    ENEMY,
    NONE;

    public static RelationType fromName(String name) {
        return EnumUtils.parseOrDefault(RelationType.class, name, NONE);
    }
}
