package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.chunk.ClientCache;
import com.github.gtexpert.blpc.common.network.MessageSyncClaims;

/** Client-side handler for single-chunk ownership sync. */
@SideOnly(Side.CLIENT)
public final class SyncClaimsClientHandler implements IMessageHandler<MessageSyncClaims, IMessage> {

    @Override
    public IMessage onMessage(MessageSyncClaims msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientCache.update(
                msg.getX(), msg.getZ(), msg.getOwner(), msg.getName(),
                msg.getPartyName(), msg.isForceLoaded()));
        return null;
    }
}
