package com.github.gtexpert.teamclaim.common.party;

public enum PartyRole {

    MEMBER,
    ADMIN,
    OWNER;

    public boolean canInvite() {
        return ordinal() >= ADMIN.ordinal();
    }

    public boolean canKick(PartyRole target) {
        return ordinal() > target.ordinal();
    }

    public boolean canEditName() {
        return this == OWNER;
    }

    public boolean canDisband() {
        return this == OWNER;
    }

    public boolean canChangeRole() {
        return this == OWNER;
    }
}
