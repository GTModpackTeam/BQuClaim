package com.github.gtexpert.blpc.common.network;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/**
 * S→C packet: notify the client that a claim or force-load attempt failed due to a limit.
 * Handler lives in {@code client.network.ClaimFailedClientHandler}.
 */
public class MessageClaimFailed implements IMessage {

    private String reason;
    private int current;
    private int max;

    public MessageClaimFailed() {}

    public MessageClaimFailed(String reason, int current, int max) {
        this.reason = reason;
        this.current = current;
        this.max = max;
    }

    public String getReason() {
        return reason;
    }

    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        reason = ByteBufUtils.readUTF8String(buf);
        current = buf.readInt();
        max = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, reason);
        buf.writeInt(current);
        buf.writeInt(max);
    }
}
