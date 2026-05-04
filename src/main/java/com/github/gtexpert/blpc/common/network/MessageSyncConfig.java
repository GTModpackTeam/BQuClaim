package com.github.gtexpert.blpc.common.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/**
 * S→C: Syncs server config (max claims/force-loads) to client.
 * Handler lives in {@code client.network.SyncConfigClientHandler}.
 */
public class MessageSyncConfig implements IMessage {

    private int maxClaims;
    private int maxForce;

    public MessageSyncConfig() {}

    public MessageSyncConfig(int maxClaims, int maxForce) {
        this.maxClaims = maxClaims;
        this.maxForce = maxForce;
    }

    public int getMaxClaims() {
        return maxClaims;
    }

    public int getMaxForce() {
        return maxForce;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.maxClaims = buf.readInt();
        this.maxForce = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.maxClaims);
        buf.writeInt(this.maxForce);
    }
}
