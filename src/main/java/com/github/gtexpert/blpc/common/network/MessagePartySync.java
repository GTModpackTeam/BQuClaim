package com.github.gtexpert.blpc.common.network;

import net.minecraft.nbt.NBTTagCompound;

/** S→C: full party snapshot. Handler: {@code client.network.PartySyncClientHandler}. */
public class MessagePartySync extends NbtMessage {

    public MessagePartySync() {}

    public MessagePartySync(NBTTagCompound data) {
        super(data);
    }
}
