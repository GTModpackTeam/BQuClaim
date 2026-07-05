package com.github.gtexpert.blpc.client.network;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.network.message.SyncConfig;

/** Client-side handler that overrides client config with server-authoritative values. */
@SideOnly(Side.CLIENT)
public final class SyncConfigClientHandler extends MainThreadMessageHandler<SyncConfig> {

    @Override
    protected void handleOnMainThread(SyncConfig msg) {
        ModConfig.claims.maxClaimsPerPlayer = msg.getMaxClaims();
        ModConfig.claims.maxForceLoadsPerPlayer = msg.getMaxForce();
    }
}
