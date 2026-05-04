package com.github.gtexpert.blpc.common.network;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/**
 * S→C: Syncs all party data as NBT to the client.
 * Handler lives in {@code client.network.PartySyncClientHandler}.
 */
public class MessagePartySync implements IMessage {

    private NBTTagCompound data;

    public MessagePartySync() {}

    public MessagePartySync(NBTTagCompound data) {
        this.data = data;
    }

    public NBTTagCompound getData() {
        return data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.data);
    }
}
