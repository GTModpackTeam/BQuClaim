package com.github.gtexpert.blpc.integration.jmap;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.common.waypoint.ClientWaypointCache;
import com.github.gtexpert.blpc.common.waypoint.PartyWaypointData;

import journeymap.client.model.Waypoint;
import journeymap.client.waypoint.WaypointStore;

/**
 * Mirrors {@link ClientWaypointCache} (party-shared waypoints synced from the server) onto the
 * local JourneyMap waypoint store, so every online party member sees the same shared markers on
 * their own map.
 * <p>
 * A waypoint built from {@code journeymap.client.api.display.Waypoint(BLPC_MODID, waypointId,
 * ...)} always ends up with {@code getId() == "blpc:" + waypointId} (confirmed from JourneyMap's
 * {@code Waypoint.getGuid()}: {@code origin + ":" + displayId}), so shared waypoints can be
 * reliably matched and cleaned up by id without needing a separate id-mapping table.
 */
@SideOnly(Side.CLIENT)
public class JMapWaypointSyncHandler {

    private static JMapWaypointSyncHandler instance;

    private final Runnable listener = this::onCacheChanged;

    public void register() {
        instance = this;
        ClientWaypointCache.addChangeListener(listener);
    }

    public void unregister() {
        ClientWaypointCache.removeChangeListener(listener);
        if (instance == this) instance = null;
    }

    /** Re-applies the mirror after a settings toggle (see {@link JMapSettingsPanel}). */
    static void refreshFromSettings() {
        if (instance != null) instance.onCacheChanged();
    }

    private void onCacheChanged() {
        JMapWaypointOutgoing.beginApplyingRemoteChange();
        try {
            applyToJourneyMap();
        } finally {
            JMapWaypointOutgoing.endApplyingRemoteChange();
        }
    }

    private void applyToJourneyMap() {
        Map<String, Waypoint> mirrored = new HashMap<>();
        for (Waypoint wp : WaypointStore.INSTANCE.getAll()) {
            if (Tags.MODID.equals(wp.getOrigin())) {
                mirrored.put(wp.getId(), wp);
            }
        }

        if (!JMapClientConfig.isWaypointSharingEnabled()) {
            // Disabled locally: drop anything BLPC previously mirrored, mirror nothing new.
            for (Waypoint leftover : mirrored.values()) {
                WaypointStore.INSTANCE.remove(leftover);
            }
            return;
        }

        for (PartyWaypointData shared : ClientWaypointCache.getAll()) {
            var apiWaypoint = new journeymap.client.api.display.Waypoint(Tags.MODID, shared.waypointId, shared.name,
                    shared.dimension, new BlockPos(shared.x, shared.y, shared.z));
            apiWaypoint.setColor(shared.color);
            Waypoint internal = new Waypoint(apiWaypoint);
            WaypointStore.INSTANCE.save(internal);
            mirrored.remove(internal.getId());
        }

        // Anything left is a BLPC-origin waypoint no longer in the cache (removed by the owner,
        // or the local player is no longer in that party) — clean it up.
        for (Waypoint leftover : mirrored.values()) {
            WaypointStore.INSTANCE.remove(leftover);
        }
    }
}
