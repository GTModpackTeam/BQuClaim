package com.github.gtexpert.blpc.client.network;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.common.network.message.WaypointAction;
import com.github.gtexpert.blpc.common.network.message.WaypointSync;
import com.github.gtexpert.blpc.common.waypoint.ClientWaypointCache;
import com.github.gtexpert.blpc.common.waypoint.PartyWaypointData;

/** Client-side handler for a single party-waypoint add/update/remove. */
@SideOnly(Side.CLIENT)
public final class WaypointSyncClientHandler extends MainThreadMessageHandler<WaypointSync> {

    @Override
    protected void handleOnMainThread(WaypointSync msg) {
        if (msg.getAction() == WaypointAction.ACTION_REMOVE) {
            ClientWaypointCache.remove(msg.getWaypointId());
            return;
        }
        ClientWaypointCache.update(new PartyWaypointData(msg.getWaypointId(), msg.getOwnerUUID(),
                msg.getName(), msg.getDimension(), msg.getX(), msg.getY(), msg.getZ(), msg.getColor()));
    }
}
