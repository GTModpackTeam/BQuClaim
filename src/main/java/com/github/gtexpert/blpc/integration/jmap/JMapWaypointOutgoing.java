package com.github.gtexpert.blpc.integration.jmap;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyRole;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.network.message.WaypointAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

import journeymap.api.v2.common.event.CommonEventRegistry;
import journeymap.api.v2.common.event.common.WaypointEvent;
import journeymap.api.v2.common.waypoint.Waypoint;

/**
 * Detects local waypoint changes via the v2 {@link WaypointEvent} API and forwards them to the
 * server so they reach the rest of the local player's party.
 * <p>
 * Death waypoints are excluded by the API itself: {@code CommonEventRegistry.WAYPOINT_EVENT}
 * suppresses {@code CREATE} for death waypoints; they fire only on the dedicated
 * {@code DEATH_WAYPOINT_EVENT}.
 * <p>
 * Only the party OWNER's edits are sent (see {@code WaypointAction} javadoc). This is purely
 * to avoid pointless traffic and rollback flicker for non-owners; the server enforces the real
 * check regardless of what a modified client might send.
 */
@SideOnly(Side.CLIENT)
public final class JMapWaypointOutgoing {

    private static volatile boolean applyingRemoteChange = false;

    private JMapWaypointOutgoing() {}

    /** Set by {@link JMapWaypointSyncHandler} while it writes incoming shared waypoints locally. */
    static void beginApplyingRemoteChange() {
        applyingRemoteChange = true;
    }

    static void endApplyingRemoteChange() {
        applyingRemoteChange = false;
    }

    static void register() {
        CommonEventRegistry.WAYPOINT_EVENT.subscribe(Tags.MODID, JMapWaypointOutgoing::onWaypointEvent);
    }

    private static void onWaypointEvent(WaypointEvent event) {
        if (applyingRemoteChange) return;
        if (!JMapClientConfig.isWaypointSharingEnabled()) return;
        if (!isPartyOwner()) return;

        Waypoint waypoint = event.getWaypoint();
        if (Tags.MODID.equals(waypoint.getModId())) return;

        switch (event.getContext()) {
            case CREATE, UPDATE -> {
                int dimension = parseDimension(waypoint.getPrimaryDimension());
                ModNetwork.INSTANCE.sendToServer(WaypointAction.addOrUpdate(
                        waypoint.getId(), waypoint.getName(), dimension,
                        waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.getColor()));
            }
            case DELETED -> ModNetwork.INSTANCE.sendToServer(WaypointAction.remove(waypoint.getId()));
            default -> {}
        }
    }

    private static int parseDimension(String dim) {
        if (dim == null || dim.isEmpty()) return 0;
        try {
            return Integer.parseInt(dim);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isPartyOwner() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return false;
        Party party = ClientPartyCache.getPartyByPlayer(mc.player.getUniqueID());
        return party != null && party.getRole(mc.player.getUniqueID()) == PartyRole.OWNER;
    }
}
