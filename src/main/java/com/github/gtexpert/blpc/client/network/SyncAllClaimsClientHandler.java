package com.github.gtexpert.blpc.client.network;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.chunk.ClientClaimCache;
import com.github.gtexpert.blpc.common.network.message.SyncAllClaims;

/** Client-side handler for the full chunk-ownership sync sent on login. */
@SideOnly(Side.CLIENT)
public final class SyncAllClaimsClientHandler extends MainThreadMessageHandler<SyncAllClaims> {

    @Override
    protected void handleOnMainThread(SyncAllClaims msg) {
        ClientClaimCache.clear();
        NBTTagCompound data = msg.getData();
        for (String key : data.getKeySet()) {
            ClaimedChunkData d = ClaimedChunkData.fromNBT(data.getCompoundTag(key));
            if (d == null) continue;
            ClientClaimCache.update(d.x, d.z, d.ownerUUID, d.ownerName, d.partyName, d.isForceLoaded);
        }
    }
}
