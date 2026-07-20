package com.github.gtexpert.blpc.common.waypoint;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

/**
 * A single party-shared waypoint. {@code waypointId} is the JourneyMap-side identifier of the
 * waypoint on the creator's client ({@code journeymap.api.v2.common.waypoint.Waypoint#getId()}),
 * reused as the shared key so add/update/remove messages from any member address the same entry.
 */
public class PartyWaypointData {

    public final String waypointId;
    public final UUID ownerUUID;
    public final String name;
    public final int dimension;
    public final int x, y, z;
    public final int color;

    public PartyWaypointData(String waypointId, UUID ownerUUID, String name, int dimension, int x, int y, int z,
                             int color) {
        this.waypointId = waypointId;
        this.ownerUUID = ownerUUID;
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", waypointId);
        tag.setUniqueId("owner", ownerUUID);
        tag.setString("name", name);
        tag.setInteger("dim", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setInteger("color", color);
        return tag;
    }

    public static PartyWaypointData fromNBT(NBTTagCompound tag) {
        UUID owner = tag.getUniqueId("owner");
        if (owner == null || owner.equals(new UUID(0L, 0L))) return null;
        return new PartyWaypointData(
                tag.getString("id"), owner, tag.getString("name"), tag.getInteger("dim"),
                tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"), tag.getInteger("color"));
    }
}
