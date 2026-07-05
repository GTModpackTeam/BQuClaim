package com.github.gtexpert.blpc.client.network;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.chunk.ClientClaimCache;
import com.github.gtexpert.blpc.common.network.message.SyncClaims;

/** Client-side handler for single-chunk ownership sync. */
@SideOnly(Side.CLIENT)
public final class SyncClaimsClientHandler extends MainThreadMessageHandler<SyncClaims> {

    @Override
    protected void handleOnMainThread(SyncClaims msg) {
        ClientClaimCache.update(msg.getX(), msg.getZ(), msg.getOwner(), msg.getName(),
                msg.getPartyName(), msg.isForceLoaded());
    }
}
