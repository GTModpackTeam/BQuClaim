package com.github.gtexpert.blpc.common.network;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import com.github.gtexpert.blpc.common.party.RelationType;

import io.netty.buffer.ByteBuf;

/**
 * S→C packet: notify the client that a player entered/left a claimed chunk.
 * Handler lives in {@code client.network.ChunkTransitNotifyClientHandler}.
 */
public class MessageChunkTransitNotify implements IMessage {

    private String playerName;
    private String relationName;
    private boolean entered;

    public MessageChunkTransitNotify() {}

    public MessageChunkTransitNotify(String playerName, RelationType relation, boolean entered) {
        this.playerName = playerName;
        this.relationName = relation.name();
        this.entered = entered;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getRelationName() {
        return relationName;
    }

    public boolean isEntered() {
        return entered;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        playerName = ByteBufUtils.readUTF8String(buf);
        relationName = ByteBufUtils.readUTF8String(buf);
        entered = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, playerName);
        ByteBufUtils.writeUTF8String(buf, relationName);
        buf.writeBoolean(entered);
    }
}
