package com.github.gtexpert.blpc.common.network.message;

import net.minecraft.nbt.NBTTagCompound;

import com.github.gtexpert.blpc.common.network.NbtMessage;

/**
 * S→C: all of the local player's party-shared waypoints, sent on login.
 * Handler: {@code client.network.SyncAllWaypointsClientHandler}.
 */
public class SyncAllWaypoints extends NbtMessage {

    public SyncAllWaypoints() {}

    public SyncAllWaypoints(NBTTagCompound data) {
        super(data);
    }
}
