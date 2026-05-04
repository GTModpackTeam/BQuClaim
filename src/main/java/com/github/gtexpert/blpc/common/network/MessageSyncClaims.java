package com.github.gtexpert.blpc.common.network;

import java.util.UUID;

import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/**
 * S→C: Updates a single chunk's ownership state.
 * Handler lives in {@code client.network.SyncClaimsClientHandler}.
 */
public class MessageSyncClaims implements IMessage {

    private int x;
    private int z;
    private UUID owner;
    private String name;
    private String partyName;
    private boolean isForceLoaded;

    public MessageSyncClaims() {}

    public MessageSyncClaims(int x, int z, UUID owner, String name, String partyName, boolean isForceLoaded) {
        this.x = x;
        this.z = z;
        this.owner = owner;
        this.name = name;
        this.partyName = partyName;
        this.isForceLoaded = isForceLoaded;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getPartyName() {
        return partyName;
    }

    public boolean isForceLoaded() {
        return isForceLoaded;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
        // Skip UUID fields when owner is null (unclaim)
        if (buf.readBoolean()) {
            this.owner = new UUID(buf.readLong(), buf.readLong());
            this.name = ByteBufUtils.readUTF8String(buf);
            this.partyName = ByteBufUtils.readUTF8String(buf);
            this.isForceLoaded = buf.readBoolean();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(z);
        boolean hasOwner = owner != null;
        buf.writeBoolean(hasOwner);
        if (hasOwner) {
            buf.writeLong(owner.getMostSignificantBits());
            buf.writeLong(owner.getLeastSignificantBits());
            ByteBufUtils.writeUTF8String(buf, name);
            ByteBufUtils.writeUTF8String(buf, partyName);
            buf.writeBoolean(isForceLoaded);
        }
    }
}
