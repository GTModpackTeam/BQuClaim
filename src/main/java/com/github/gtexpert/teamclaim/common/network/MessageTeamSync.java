package com.github.gtexpert.teamclaim.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.github.gtexpert.teamclaim.common.party.ClientPartyCache;

import io.netty.buffer.ByteBuf;

public class MessageTeamSync implements IMessage {

    private NBTTagCompound data;

    public MessageTeamSync() {}

    public MessageTeamSync(NBTTagCompound data) {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.data);
    }

    public static class Handler implements IMessageHandler<MessageTeamSync, IMessage> {

        @Override
        public IMessage onMessage(MessageTeamSync message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientPartyCache.loadFromNBT(message.data);
            });
            return null;
        }
    }
}
