package com.github.gtexpert.blpc.common.network;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import io.netty.buffer.ByteBuf;

/** Base for messages whose entire payload is a single NBT compound. */
public abstract class NbtMessage implements IMessage {

    protected NBTTagCompound data;

    protected NbtMessage() {}

    protected NbtMessage(NBTTagCompound data) {
        this.data = data;
    }

    public NBTTagCompound getData() {
        return data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        this.data = tag != null ? tag : new NBTTagCompound();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.data);
    }
}
