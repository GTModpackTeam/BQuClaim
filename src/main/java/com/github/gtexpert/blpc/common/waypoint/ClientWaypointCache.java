package com.github.gtexpert.blpc.common.waypoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side in-memory cache of the local player's party-shared waypoints.
 * Populated via {@code WaypointSync} / {@code SyncAllWaypoints} from the server.
 */
public class ClientWaypointCache {

    private static final Map<String, PartyWaypointData> cache = new HashMap<>();
    private static final List<Runnable> changeListeners = new ArrayList<>();

    public static void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public static void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private static void fireChangeListeners() {
        for (Runnable listener : new ArrayList<>(changeListeners)) {
            listener.run();
        }
    }

    public static void update(PartyWaypointData waypoint) {
        cache.put(waypoint.waypointId, waypoint);
        fireChangeListeners();
    }

    /**
     * Replaces the entire cache and fires listeners exactly once. Used for the full login sync —
     * calling {@link #update(PartyWaypointData)} per entry would fire the JourneyMap mirror
     * listener (which re-scans every waypoint) once per waypoint, an O(n^2) cost on large lists.
     */
    public static void loadAll(Collection<PartyWaypointData> waypoints) {
        cache.clear();
        for (PartyWaypointData waypoint : waypoints) {
            cache.put(waypoint.waypointId, waypoint);
        }
        fireChangeListeners();
    }

    public static void remove(String waypointId) {
        cache.remove(waypointId);
        fireChangeListeners();
    }

    public static void clearAll() {
        cache.clear();
        changeListeners.clear();
    }

    public static Collection<PartyWaypointData> getAll() {
        return Collections.unmodifiableCollection(cache.values());
    }
}
