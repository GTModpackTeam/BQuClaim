package com.github.gtexpert.blpc.client.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.chunk.ClientCache;
import com.github.gtexpert.blpc.common.network.MessageSyncAllClaims;

/** Client-side handler for the full chunk-ownership sync sent on login. */
@SideOnly(Side.CLIENT)
public final class SyncAllClaimsClientHandler implements IMessageHandler<MessageSyncAllClaims, IMessage> {

    @Override
    public IMessage onMessage(MessageSyncAllClaims msg, MessageContext ctx) {
        final NBTTagCompound data = msg.getData();
        Minecraft.getMinecraft().addScheduledTask(() -> {
            ClientCache.clear();
            for (String key : data.getKeySet()) {
                ClaimedChunkData d = ClaimedChunkData.fromNBT(data.getCompoundTag(key));
                if (d == null) continue;
                ClientCache.update(d.x, d.z, d.ownerUUID, d.ownerName, d.partyName, d.isForceLoaded);
            }
        });
        return null;
    }
}
