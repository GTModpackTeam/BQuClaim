package com.github.gtexpert.blpc.common.network;

import java.util.UUID;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/**
 * C→S packet for party operations. A single message multiplexes all party
 * mutations through an integer {@code action} discriminator and a
 * {@code stringArg} payload.
 * <p>
 * <b>Wire protocol stability:</b> the {@code ACTION_*} constants are part of
 * the on-wire format. Do not renumber existing actions; append new ones at the
 * end. Removing an action requires a coordinated client/server release.
 * <p>
 * <b>Server handler:</b>
 * {@link com.github.gtexpert.blpc.common.network.party.PartyActionDispatcher}.
 * Use the static factory methods (e.g. {@link #create}, {@link #invite}) when
 * sending from the client — they encode arguments consistently with the
 * server-side decoder.
 */
public class MessagePartyAction implements IMessage {

    public static final int ACTION_CREATE = 0;
    public static final int ACTION_DISBAND = 1;
    public static final int ACTION_RENAME = 2;
    public static final int ACTION_INVITE = 3;
    public static final int ACTION_ACCEPT_INVITE = 4;
    public static final int ACTION_KICK_OR_LEAVE = 5;
    public static final int ACTION_CHANGE_ROLE = 6;
    public static final int ACTION_TOGGLE_BQU_LINK = 7;
    public static final int ACTION_TOGGLE_FAKE_PLAYERS = 8;
    public static final int ACTION_TOGGLE_EXPLOSION_PROTECTION = 9;
    public static final int ACTION_ADD_ALLY = 10;
    public static final int ACTION_REMOVE_ALLY = 11;
    public static final int ACTION_ADD_ENEMY = 12;
    public static final int ACTION_REMOVE_ENEMY = 13;
    public static final int ACTION_TRANSFER_OWNERSHIP = 14;
    public static final int ACTION_SET_TRUST_LEVEL = 15;
    public static final int ACTION_SET_FAKEPLAYER_TRUST = 16;
    public static final int ACTION_SET_FREE_TO_JOIN = 17;
    public static final int ACTION_SET_COLOR = 18;
    public static final int ACTION_SET_DESCRIPTION = 19;
    public static final int ACTION_JOIN_FREE_PARTY = 20;
    public static final int ACTION_SET_MAX_MEMBERS = 21;

    private int action;
    private String stringArg;

    public MessagePartyAction() {}

    public MessagePartyAction(int action, String stringArg) {
        this.action = action;
        this.stringArg = stringArg;
    }

    public int getAction() {
        return action;
    }

    public String getStringArg() {
        return stringArg;
    }

    public static MessagePartyAction create(String name) {
        return new MessagePartyAction(ACTION_CREATE, name);
    }

    public static MessagePartyAction disband() {
        return new MessagePartyAction(ACTION_DISBAND, "");
    }

    public static MessagePartyAction rename(String newName) {
        return new MessagePartyAction(ACTION_RENAME, newName);
    }

    public static MessagePartyAction invite(String username) {
        return new MessagePartyAction(ACTION_INVITE, username);
    }

    public static MessagePartyAction acceptInvite(UUID partyId) {
        return new MessagePartyAction(ACTION_ACCEPT_INVITE, partyId.toString());
    }

    public static MessagePartyAction kickOrLeave(String username) {
        return new MessagePartyAction(ACTION_KICK_OR_LEAVE, username);
    }

    public static MessagePartyAction changeRole(String usernameAndRole) {
        return new MessagePartyAction(ACTION_CHANGE_ROLE, usernameAndRole);
    }

    public static MessagePartyAction toggleBQuLink(boolean linked) {
        return new MessagePartyAction(ACTION_TOGGLE_BQU_LINK, linked ? "true" : "false");
    }

    public static MessagePartyAction setExplosionProtection(boolean protect) {
        return new MessagePartyAction(ACTION_TOGGLE_EXPLOSION_PROTECTION, protect ? "true" : "false");
    }

    public static MessagePartyAction addAlly(UUID partyId) {
        return new MessagePartyAction(ACTION_ADD_ALLY, partyId.toString());
    }

    public static MessagePartyAction removeAlly(UUID partyId) {
        return new MessagePartyAction(ACTION_REMOVE_ALLY, partyId.toString());
    }

    public static MessagePartyAction addEnemy(UUID partyId) {
        return new MessagePartyAction(ACTION_ADD_ENEMY, partyId.toString());
    }

    public static MessagePartyAction removeEnemy(UUID partyId) {
        return new MessagePartyAction(ACTION_REMOVE_ENEMY, partyId.toString());
    }

    public static MessagePartyAction transferOwnership(String username) {
        return new MessagePartyAction(ACTION_TRANSFER_OWNERSHIP, username);
    }

    public static MessagePartyAction setTrustLevel(String actionAndLevel) {
        return new MessagePartyAction(ACTION_SET_TRUST_LEVEL, actionAndLevel);
    }

    public static MessagePartyAction setFakePlayerTrust(String level) {
        return new MessagePartyAction(ACTION_SET_FAKEPLAYER_TRUST, level);
    }

    public static MessagePartyAction setFreeToJoin(boolean free) {
        return new MessagePartyAction(ACTION_SET_FREE_TO_JOIN, free ? "true" : "false");
    }

    public static MessagePartyAction setColor(int color) {
        return new MessagePartyAction(ACTION_SET_COLOR, Integer.toString(color));
    }

    public static MessagePartyAction setDescription(String desc) {
        return new MessagePartyAction(ACTION_SET_DESCRIPTION, desc);
    }

    public static MessagePartyAction joinFreeParty(UUID partyId) {
        return new MessagePartyAction(ACTION_JOIN_FREE_PARTY, partyId.toString());
    }

    public static MessagePartyAction setMaxMembers(int max) {
        return new MessagePartyAction(ACTION_SET_MAX_MEMBERS, Integer.toString(max));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        stringArg = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        ByteBufUtils.writeUTF8String(buf, stringArg);
    }
}
