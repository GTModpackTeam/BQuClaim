package com.github.gtexpert.blpc.client.network;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.network.message.PartySync;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/** Client-side handler that loads the full party snapshot into the client cache. */
@SideOnly(Side.CLIENT)
public final class PartySyncClientHandler extends MainThreadMessageHandler<PartySync> {

    @Override
    protected void handleOnMainThread(PartySync msg) {
        ClientPartyCache.loadFromNBT(msg.getData());
    }
}
