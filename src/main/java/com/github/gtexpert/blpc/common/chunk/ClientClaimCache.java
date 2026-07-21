package com.github.gtexpert.blpc.common.chunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.gtexpert.blpc.common.chunk.ChunkManagerData.ChunkKey;

/**
 * Client-side in-memory cache of chunk claim data.
 * Populated via {@code SyncClaims} / {@code SyncAllClaims} from the server.
 */
public class ClientClaimCache {

    private static final Map<ChunkKey, ClaimedChunkData> cache = new HashMap<>();
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

    public static void update(
                              int x, int z, int dim, UUID owner, String name, String partyName, boolean isForceLoaded) {
        ChunkKey key = ChunkManagerData.chunkKey(x, z, dim);
        if (owner == null) {
            cache.remove(key);
        } else {
            cache.put(key, new ClaimedChunkData(x, z, dim, owner, name, partyName, isForceLoaded));
        }
        fireChangeListeners();
    }

    public static ClaimedChunkData get(int x, int z, int dim) {
        return cache.get(ChunkManagerData.chunkKey(x, z, dim));
    }

    public static void clear() {
        cache.clear();
        fireChangeListeners();
    }

    public static void clearAll() {
        cache.clear();
        changeListeners.clear();
    }

    public static Collection<ClaimedChunkData> getAll() {
        return cache.values();
    }
}
