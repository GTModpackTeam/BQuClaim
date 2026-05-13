package com.github.gtexpert.blpc.common.network;

import net.minecraft.nbt.NBTTagCompound;

/** S→C: all chunk ownership data on login. Handler: {@code client.network.SyncAllClaimsClientHandler}. */
public class MessageSyncAllClaims extends NbtMessage {

    public MessageSyncAllClaims() {}

    public MessageSyncAllClaims(NBTTagCompound data) {
        super(data);
    }
}
