package com.github.gtexpert.blpc.common.network;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/**
 * S→C packet: notify the client of a party event (join, leave, kick, disband, etc.).
 * Handler lives in {@code client.network.PartyEventNotifyClientHandler}.
 */
public class MessagePartyEventNotify implements IMessage {

    public static final String MEMBER_JOINED = "MEMBER_JOINED";
    public static final String MEMBER_LEFT = "MEMBER_LEFT";
    public static final String KICKED = "KICKED";
    public static final String DISBANDED = "DISBANDED";
    public static final String INVITE_RECEIVED = "INVITE_RECEIVED";
    public static final String OWNER_TRANSFERRED = "OWNER_TRANSFERRED";
    public static final String ROLE_CHANGED = "ROLE_CHANGED";
    public static final String BQU_LINKED = "BQU_LINKED";
    public static final String BQU_UNLINKED = "BQU_UNLINKED";
    public static final String PARTY_FULL = "PARTY_FULL";

    private String eventType;
    private String playerName;
    private String extraInfo;

    public MessagePartyEventNotify() {}

    public MessagePartyEventNotify(String eventType, String playerName, String extraInfo) {
        this.eventType = eventType;
        this.playerName = playerName;
        this.extraInfo = extraInfo != null ? extraInfo : "";
    }

    public String getEventType() {
        return eventType;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        eventType = ByteBufUtils.readUTF8String(buf);
        playerName = ByteBufUtils.readUTF8String(buf);
        extraInfo = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, eventType);
        ByteBufUtils.writeUTF8String(buf, playerName);
        ByteBufUtils.writeUTF8String(buf, extraInfo);
    }
}
