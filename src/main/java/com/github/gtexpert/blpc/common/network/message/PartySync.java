package com.github.gtexpert.blpc.common.network.message;

import net.minecraft.nbt.NBTTagCompound;

import com.github.gtexpert.blpc.common.network.NbtMessage;

/** S→C: full party snapshot. Handler: {@code client.network.PartySyncClientHandler}. */
public class PartySync extends NbtMessage {

    public PartySync() {}

    public PartySync(NBTTagCompound data) {
        super(data);
    }
}
