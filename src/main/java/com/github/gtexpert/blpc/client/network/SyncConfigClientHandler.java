package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.network.MessageSyncConfig;

/** Client-side handler that overrides client config with server-authoritative values. */
@SideOnly(Side.CLIENT)
public final class SyncConfigClientHandler implements IMessageHandler<MessageSyncConfig, IMessage> {

    @Override
    public IMessage onMessage(MessageSyncConfig msg, MessageContext ctx) {
        final int maxClaims = msg.getMaxClaims();
        final int maxForce = msg.getMaxForce();
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ModConfig.claims.maxClaimsPerPlayer = maxClaims;
            ModConfig.claims.maxForceLoadsPerPlayer = maxForce;
        });
        return null;
    }
}
