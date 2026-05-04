package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.network.MessagePartySync;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/** Client-side handler that loads the full party snapshot into the client cache. */
@SideOnly(Side.CLIENT)
public final class PartySyncClientHandler implements IMessageHandler<MessagePartySync, IMessage> {

    @Override
    public IMessage onMessage(MessagePartySync msg, MessageContext ctx) {
        final NBTTagCompound data = msg.getData();
        Minecraft.getMinecraft().addScheduledTask(() -> ClientPartyCache.loadFromNBT(data));
        return null;
    }
}
