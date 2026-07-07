package com.github.gtexpert.blpc.integration.jmap;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyRole;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.network.message.WaypointAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

import journeymap.client.model.Waypoint;

/**
 * Detects local waypoint changes ({@link WaypointStoreMixin}) and forwards them to the server
 * so they reach the rest of the local player's party. Not registered/loaded unless JourneyMap
 * is present (gated by {@code mixins.blpc.journeymap.json}).
 * <p>
 * {@code WaypointEditor.save()} always calls {@code WaypointStore.remove(original)} then
 * {@code WaypointStore.save(edited)}, even for a brand-new waypoint. Without debouncing, every
 * edit would send a spurious REMOVE immediately followed by an ADD_OR_UPDATE. Instead, a
 * detected remove is held until the end of the current client tick; if a save for the same
 * waypoint arrives before then, the pending remove is dropped and only the update is sent.
 * Removes are held per waypoint id (not a single slot), so deleting several waypoints within
 * one tick still forwards every one of them.
 */
@SideOnly(Side.CLIENT)
public final class JMapWaypointOutgoing {

    private static volatile boolean applyingRemoteChange = false;
    private static final Set<String> pendingRemoveIds = ConcurrentHashMap.newKeySet();

    private JMapWaypointOutgoing() {}

    /** Set by {@link JMapWaypointSyncHandler} while it writes incoming shared waypoints locally. */
    static void beginApplyingRemoteChange() {
        applyingRemoteChange = true;
    }

    static void endApplyingRemoteChange() {
        applyingRemoteChange = false;
    }

    public static void onLocalSave(Waypoint waypoint) {
        if (applyingRemoteChange) return;
        if (!JMapClientConfig.isWaypointSharingEnabled()) return;
        if (waypoint.getType() == Waypoint.Type.Death) return;
        if (!isPartyOwner()) return;

        // Drop only this waypoint's pending remove, leaving other waypoints' removes queued.
        pendingRemoveIds.remove(waypoint.getId());

        Integer color = waypoint.getColor();
        // A waypoint can span multiple dimensions in JourneyMap's UI, but BLPC's wire format
        // only carries one; picking any single entry is fine since shared waypoints are almost
        // always dimension-specific in practice.
        int dimension = waypoint.getDimensions().isEmpty() ? 0 : waypoint.getDimensions().iterator().next();
        ModNetwork.INSTANCE.sendToServer(WaypointAction.addOrUpdate(
                waypoint.getId(), waypoint.getName(), dimension,
                waypoint.getX(), waypoint.getY(), waypoint.getZ(), color != null ? color : 0xFFFFFF));
    }

    public static void onLocalRemove(Waypoint waypoint) {
        if (applyingRemoteChange) return;
        if (!JMapClientConfig.isWaypointSharingEnabled()) return;
        if (waypoint.getType() == Waypoint.Type.Death) return;
        if (!isPartyOwner()) return;

        // Held until end-of-tick — see class javadoc. A same-waypoint save() arriving first
        // clears this via onLocalSave, so a pure edit never sends a REMOVE at all.
        pendingRemoveIds.add(waypoint.getId());
    }

    /**
     * Client-side mirror of the server's authorization check (see {@code WaypointAction}
     * javadoc) — only the party OWNER's edits are sent. This is purely to avoid pointless
     * traffic and rollback flicker for non-owners; the server enforces the real check
     * regardless of what a modified client might send.
     */
    private static boolean isPartyOwner() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return false;
        Party party = ClientPartyCache.getPartyByPlayer(mc.player.getUniqueID());
        return party != null && party.getRole(mc.player.getUniqueID()) == PartyRole.OWNER;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pendingRemoveIds.isEmpty()) return;
        // Drain via iterator.remove() so a remove queued mid-drain isn't lost.
        for (Iterator<String> it = pendingRemoveIds.iterator(); it.hasNext();) {
            String toRemove = it.next();
            it.remove();
            ModNetwork.INSTANCE.sendToServer(WaypointAction.remove(toRemove));
        }
    }
}
