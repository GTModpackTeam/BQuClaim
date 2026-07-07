package com.github.gtexpert.blpc.common.waypoint;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side storage for party-shared waypoints. Singleton, persisted by
 * {@link com.github.gtexpert.blpc.common.BLPCSaveHandler}. Waypoints are grouped per party;
 * a party with no waypoints has no entry in {@link #partyWaypoints}.
 */
public class WaypointManagerData {

    private static volatile WaypointManagerData instance;

    private final Map<UUID, Map<String, PartyWaypointData>> partyWaypoints = new ConcurrentHashMap<>();

    public static synchronized WaypointManagerData getInstance() {
        if (instance == null) {
            instance = new WaypointManagerData();
        }
        return instance;
    }

    public static synchronized void reset() {
        instance = new WaypointManagerData();
    }

    public Collection<PartyWaypointData> getWaypoints(UUID partyId) {
        Map<String, PartyWaypointData> waypoints = partyWaypoints.get(partyId);
        return waypoints == null ? Collections.emptyList() : Collections.unmodifiableCollection(waypoints.values());
    }

    public PartyWaypointData getWaypoint(UUID partyId, String waypointId) {
        Map<String, PartyWaypointData> waypoints = partyWaypoints.get(partyId);
        return waypoints == null ? null : waypoints.get(waypointId);
    }

    public int countWaypoints(UUID partyId) {
        Map<String, PartyWaypointData> waypoints = partyWaypoints.get(partyId);
        return waypoints == null ? 0 : waypoints.size();
    }

    public void setWaypoint(UUID partyId, PartyWaypointData waypoint) {
        partyWaypoints.computeIfAbsent(partyId, k -> new ConcurrentHashMap<>())
                .put(waypoint.waypointId, waypoint);
    }

    public void removeWaypoint(UUID partyId, String waypointId) {
        Map<String, PartyWaypointData> waypoints = partyWaypoints.get(partyId);
        if (waypoints == null) return;
        waypoints.remove(waypointId);
        if (waypoints.isEmpty()) {
            partyWaypoints.remove(partyId);
        }
    }

    /** Removes every waypoint belonging to a disbanded party. */
    public void removeParty(UUID partyId) {
        partyWaypoints.remove(partyId);
    }

    public Map<UUID, Map<String, PartyWaypointData>> getAllForSave() {
        return Collections.unmodifiableMap(partyWaypoints);
    }

    public void loadParty(UUID partyId, Map<String, PartyWaypointData> waypoints) {
        if (waypoints.isEmpty()) return;
        partyWaypoints.put(partyId, new ConcurrentHashMap<>(waypoints));
    }
}
