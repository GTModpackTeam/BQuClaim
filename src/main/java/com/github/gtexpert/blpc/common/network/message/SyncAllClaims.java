package com.github.gtexpert.blpc.common.network.message;

import net.minecraft.nbt.NBTTagCompound;

import com.github.gtexpert.blpc.common.network.NbtMessage;

/** S→C: all chunk ownership data on login. Handler: {@code client.network.SyncAllClaimsClientHandler}. */
public class SyncAllClaims extends NbtMessage {

    public SyncAllClaims() {}

    public SyncAllClaims(NBTTagCompound data) {
        super(data);
    }
}
