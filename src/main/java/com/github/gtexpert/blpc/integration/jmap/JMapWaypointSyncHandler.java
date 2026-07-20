package com.github.gtexpert.blpc.integration.jmap;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.waypoint.ClientWaypointCache;
import com.github.gtexpert.blpc.common.waypoint.PartyWaypointData;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import journeymap.api.v2.common.waypoint.WaypointGroup;

/**
 * Mirrors {@link ClientWaypointCache} (party-shared waypoints synced from the server) onto a
 * locked JourneyMap {@link WaypointGroup} named "BLPC Party", so every online party member sees
 * the same shared markers on their map. The group is locked so users cannot accidentally delete
 * shared waypoints through JourneyMap's UI.
 * <p>
 * Sync triggers:
 * <ul>
 * <li>Immediately when {@link ClientWaypointCache} fires a change listener (event-driven).</li>
 * <li>Periodically every N client ticks (configurable via
 * {@code ModConfig.integration.jmapWaypointSyncInterval}; 0 disables periodic sync).</li>
 * </ul>
 * Lifecycle:
 * <ul>
 * <li>Created when shared waypoints arrive (party join, server sync on login).</li>
 * <li>Updated on every cache change or periodic tick.</li>
 * <li>Removed when the cache empties (party leave, disband) or sharing is disabled.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public class JMapWaypointSyncHandler {

    static final String GROUP_NAME = "BLPC Party";

    private static JMapWaypointSyncHandler instance;

    private final Runnable listener = this::onCacheChanged;
    private int tickCounter;

    public void register() {
        instance = this;
        tickCounter = 0;
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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int interval = JMapClientConfig.getWaypointSyncInterval();
        if (interval <= 0) return;
        if (++tickCounter >= interval) {
            tickCounter = 0;
            sync();
        }
    }

    private void onCacheChanged() {
        tickCounter = 0;
        sync();
    }

    private void sync() {
        JMapWaypointOutgoing.beginApplyingRemoteChange();
        try {
            applyToJourneyMap();
        } finally {
            JMapWaypointOutgoing.endApplyingRemoteChange();
        }
    }

    private void applyToJourneyMap() {
        IClientAPI api = JMapPlugin.getApi();
        if (api == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        boolean inParty = ClientPartyCache.getPartyByPlayer(mc.player.getUniqueID()) != null;

        if (!inParty) {
            removeGroupIfPresent(api);
            return;
        }

        boolean enabled = JMapClientConfig.isWaypointSharingEnabled();
        var sharedWaypoints = ClientWaypointCache.getAll();
        WaypointGroup group = ensureGroup(api);
        group.setEnabled(enabled);

        Map<String, Waypoint> existing = new HashMap<>();
        for (Waypoint wp : api.getWaypoints(Tags.MODID)) {
            if (group.getGuid().equals(wp.getGroupId())) {
                existing.put(wp.getId(), wp);
            }
        }

        for (PartyWaypointData shared : sharedWaypoints) {
            var waypoint = WaypointFactory.createWaypoint(
                    Tags.MODID,
                    new BlockPos(shared.x, shared.y, shared.z),
                    shared.name,
                    String.valueOf(shared.dimension),
                    true);
            waypoint.setColor(shared.color);
            group.addWaypoint(waypoint);
            api.addWaypoint(Tags.MODID, waypoint);
            existing.remove(waypoint.getId());
        }

        for (Waypoint leftover : existing.values()) {
            api.removeWaypoint(Tags.MODID, leftover);
        }
    }

    private WaypointGroup ensureGroup(IClientAPI api) {
        WaypointGroup group = api.getWaypointGroupByName(Tags.MODID, GROUP_NAME);
        if (group != null) return group;

        group = WaypointFactory.createWaypointGroup(Tags.MODID, GROUP_NAME);
        group.setLocked(true);
        group.setPersistent(false);
        api.addWaypointGroup(group);
        return group;
    }

    private void removeGroupIfPresent(IClientAPI api) {
        WaypointGroup group = api.getWaypointGroupByName(Tags.MODID, GROUP_NAME);
        if (group != null) {
            api.removeWaypointGroup(group, true);
        }
    }
}
