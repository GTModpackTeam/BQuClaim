package com.github.gtexpert.blpc.integration.jmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.chunk.ClientCache;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;

/**
 * Draws claim overlays on JourneyMap. Contiguous chunks owned by the same player are
 * merged into a single polygon (outer perimeter + holes) with one label, instead of one
 * bordered-and-labelled box per chunk.
 */
@ClientPlugin
public class BLPCJourneyMapPlugin implements IClientPlugin {

    private static final String OVERLAY_GROUP = "BLPC Claims";
    private static final int CLAIM_Y = 70;
    private static final int BLOCKS_PER_CHUNK = 16;
    private static final int COLOR_OWN = 0x00FF00;
    private static final int COLOR_PARTY = 0x00FFFF;
    private static final int COLOR_OTHER = 0xFF0000;
    private static final float FILL_OPACITY = 0.35f;
    private static final float STROKE_OPACITY = 0.6f;

    private IClientAPI api;
    private static BLPCJourneyMapPlugin instance;
    private final Map<String, PolygonOverlay> activeOverlays = new HashMap<>();

    @Override
    public void initialize(IClientAPI jmClientApi) {
        this.api = jmClientApi;
        instance = this;
        api.subscribe(getModId(), EnumSet.of(
                ClientEvent.Type.DISPLAY_UPDATE,
                ClientEvent.Type.MAPPING_STARTED,
                ClientEvent.Type.MAPPING_STOPPED));
    }

    @Override
    public String getModId() {
        return Tags.MODID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        switch (event.type) {
            case DISPLAY_UPDATE, MAPPING_STARTED -> refreshOverlays(event.dimension);
            case MAPPING_STOPPED -> clearOverlays();
            default -> {}
        }
    }

    static BLPCJourneyMapPlugin getInstance() {
        return instance;
    }

    /** Re-applies overlays after a settings toggle: redraws when enabled, clears when disabled. */
    static void refreshFromSettings() {
        if (instance == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        instance.refreshOverlays(mc.player.dimension);
    }

    void refreshOverlays(int dimension) {
        if (api == null) return;

        // Client toggle (Addons → JourneyMap): drop existing overlays when disabled.
        if (!JMapClientConfig.isShowClaimOverlays()) {
            if (!activeOverlays.isEmpty()) clearOverlays();
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        UUID playerUUID = mc.player.getUniqueID();

        // Group claims by owner so each owner's contiguous chunks can be merged.
        Map<UUID, List<ClaimedChunkData>> byOwner = new HashMap<>();
        for (ClaimedChunkData claim : ClientCache.getAll()) {
            byOwner.computeIfAbsent(claim.ownerUUID, k -> new ArrayList<>()).add(claim);
        }

        Set<String> currentKeys = new HashSet<>();
        for (Map.Entry<UUID, List<ClaimedChunkData>> entry : byOwner.entrySet()) {
            buildOwnerRegions(entry.getValue(), playerUUID, dimension, currentKeys);
        }

        // Remove overlays for regions that no longer exist.
        activeOverlays.entrySet().removeIf(e -> {
            if (!currentKeys.contains(e.getKey())) {
                api.remove(e.getValue());
                return true;
            }
            return false;
        });
    }

    private void buildOwnerRegions(List<ClaimedChunkData> claims, UUID playerUUID, int dimension,
                                   Set<String> currentKeys) {
        // Index the owner's chunks by packed cell key for connectivity lookup.
        Map<Long, ClaimedChunkData> cells = new HashMap<>();
        for (ClaimedChunkData c : claims) {
            cells.put(cell(c.x, c.z), c);
        }

        ClaimedChunkData sample = claims.get(0);
        int areaColor = resolveAreaColor(sample, playerUUID);
        int textColor = resolveTextColor(sample);

        Set<Long> remaining = new HashSet<>(cells.keySet());
        while (!remaining.isEmpty()) {
            Set<Long> component = floodFill(remaining.iterator().next(), remaining);

            boolean allForceLoaded = true;
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            for (long cellKey : component) {
                ClaimedChunkData c = cells.get(cellKey);
                if (!c.isForceLoaded) allForceLoaded = false;
                minX = Math.min(minX, c.x);
                minZ = Math.min(minZ, c.z);
            }

            List<MapPolygon> loops = traceLoops(component);
            if (loops.isEmpty()) continue;
            // The largest loop is the outer perimeter; the rest are holes.
            MapPolygon outer = loops.remove(largestLoopIndex(loops));

            String key = sample.ownerUUID + ":" + minX + "," + minZ;
            currentKeys.add(key);
            String title = buildTitle(sample, allForceLoaded);
            showRegion(key, dimension, outer, loops, areaColor, textColor, title);
        }
    }

    private void showRegion(String key, int dimension, MapPolygon outer, List<MapPolygon> holes,
                            int areaColor, int textColor, String title) {
        PolygonOverlay existing = activeOverlays.get(key);
        if (existing != null) {
            existing.setOuterArea(outer);
            existing.setHoles(holes.isEmpty() ? null : holes);
            existing.setShapeProperties(createShapeProperties(areaColor));
            existing.setTitle(title);
            existing.getTextProperties().setColor(textColor);
            try {
                api.show(existing);
            } catch (Exception ignored) {}
            return;
        }

        PolygonOverlay overlay = new PolygonOverlay(getModId(), "claim_region_" + key, dimension,
                createShapeProperties(areaColor), outer, holes.isEmpty() ? null : holes);
        overlay.setOverlayGroupName(OVERLAY_GROUP);
        overlay.setTitle(title);
        overlay.setTextProperties(new TextProperties()
                .setMinZoom(4)
                .setMaxZoom(8)
                .setColor(textColor)
                .setBackgroundOpacity(0.6f)
                .setFontShadow(true));
        activeOverlays.put(key, overlay);
        try {
            api.show(overlay);
        } catch (Exception ignored) {}
    }

    private void clearOverlays() {
        if (api == null) return;
        api.removeAll(getModId(), DisplayType.Polygon);
        activeOverlays.clear();
    }

    // --- Geometry: merge contiguous chunks into outline polygons ---

    private static long cell(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** 4-connected flood fill; removes visited cells from {@code remaining}. */
    private static Set<Long> floodFill(long start, Set<Long> remaining) {
        Set<Long> component = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(start);
        remaining.remove(start);
        while (!queue.isEmpty()) {
            long c = queue.poll();
            component.add(c);
            int cx = (int) (c >> 32);
            int cz = (int) c;
            long[] neighbors = { cell(cx + 1, cz), cell(cx - 1, cz), cell(cx, cz + 1), cell(cx, cz - 1) };
            for (long n : neighbors) {
                if (remaining.remove(n)) queue.add(n);
            }
        }
        return component;
    }

    /**
     * Traces the boundary of a connected cell set into closed loops. The largest loop is the
     * outer perimeter; the rest are holes. Each cell edge whose neighbor is outside the set is a
     * directed boundary edge (oriented to match JourneyMap's single-chunk winding).
     */
    private static List<MapPolygon> traceLoops(Set<Long> cells) {
        // start corner -> ends of boundary edges
        Map<Long, Deque<Long>> edges = new HashMap<>();
        for (long c : cells) {
            int cx = (int) (c >> 32);
            int cz = (int) c;
            // south edge: (cx, cz+1) -> (cx+1, cz+1), neighbor (cx, cz+1)
            if (!cells.contains(cell(cx, cz + 1))) addEdge(edges, corner(cx, cz + 1), corner(cx + 1, cz + 1));
            // east edge: (cx+1, cz+1) -> (cx+1, cz), neighbor (cx+1, cz)
            if (!cells.contains(cell(cx + 1, cz))) addEdge(edges, corner(cx + 1, cz + 1), corner(cx + 1, cz));
            // north edge: (cx+1, cz) -> (cx, cz), neighbor (cx, cz-1)
            if (!cells.contains(cell(cx, cz - 1))) addEdge(edges, corner(cx + 1, cz), corner(cx, cz));
            // west edge: (cx, cz) -> (cx, cz+1), neighbor (cx-1, cz)
            if (!cells.contains(cell(cx - 1, cz))) addEdge(edges, corner(cx, cz), corner(cx, cz + 1));
        }

        List<MapPolygon> loops = new ArrayList<>();
        while (!edges.isEmpty()) {
            long startCorner = firstStart(edges);
            List<long[]> corners = new ArrayList<>();
            long cur = startCorner;
            do {
                Deque<Long> ends = edges.get(cur);
                long next = ends.poll();
                if (ends.isEmpty()) edges.remove(cur);
                corners.add(decode(cur));
                cur = next;
            } while (cur != startCorner);

            List<long[]> simplified = dropCollinear(corners);
            if (simplified.size() < 3) continue;
            loops.add(toPolygon(simplified));
        }
        return loops;
    }

    private static void addEdge(Map<Long, Deque<Long>> edges, long start, long end) {
        edges.computeIfAbsent(start, k -> new ArrayDeque<>()).add(end);
    }

    private static long firstStart(Map<Long, Deque<Long>> edges) {
        return edges.keySet().iterator().next();
    }

    /** Returns index of the loop with the largest absolute area (the outer perimeter). */
    private static int largestLoopIndex(List<MapPolygon> loops) {
        int best = 0;
        double bestArea = -1;
        for (int i = 0; i < loops.size(); i++) {
            double a = Math.abs(signedArea(loops.get(i)));
            if (a > bestArea) {
                bestArea = a;
                best = i;
            }
        }
        return best;
    }

    private static double signedArea(MapPolygon poly) {
        List<BlockPos> pts = poly.getPoints();
        double area = 0;
        for (int i = 0; i < pts.size(); i++) {
            BlockPos a = pts.get(i);
            BlockPos b = pts.get((i + 1) % pts.size());
            area += (double) a.getX() * b.getZ() - (double) b.getX() * a.getZ();
        }
        return area / 2.0;
    }

    /** Removes points that are collinear with their neighbors (axis-aligned loop). */
    private static List<long[]> dropCollinear(List<long[]> corners) {
        int n = corners.size();
        List<long[]> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            long[] prev = corners.get((i - 1 + n) % n);
            long[] cur = corners.get(i);
            long[] next = corners.get((i + 1) % n);
            long cross = (cur[0] - prev[0]) * (next[1] - cur[1]) - (cur[1] - prev[1]) * (next[0] - cur[0]);
            if (cross != 0) out.add(cur);
        }
        return out;
    }

    private static MapPolygon toPolygon(List<long[]> corners) {
        // Rotate so the south-west-most corner (min X, then max Z) comes first, as JourneyMap expects.
        int startIdx = 0;
        for (int i = 1; i < corners.size(); i++) {
            long[] c = corners.get(i);
            long[] s = corners.get(startIdx);
            if (c[0] < s[0] || (c[0] == s[0] && c[1] > s[1])) startIdx = i;
        }
        List<BlockPos> points = new ArrayList<>(corners.size());
        for (int i = 0; i < corners.size(); i++) {
            long[] c = corners.get((startIdx + i) % corners.size());
            points.add(new BlockPos((int) c[0] * BLOCKS_PER_CHUNK, CLAIM_Y, (int) c[1] * BLOCKS_PER_CHUNK));
        }
        return new MapPolygon(points);
    }

    private static long corner(int gx, int gz) {
        return ((long) gx << 32) | (gz & 0xFFFFFFFFL);
    }

    private static long[] decode(long c) {
        return new long[] { (int) (c >> 32), (int) c };
    }

    // --- Color / label ---

    private static ShapeProperties createShapeProperties(int color) {
        return new ShapeProperties()
                .setStrokeColor(color)
                .setStrokeOpacity(STROKE_OPACITY)
                .setStrokeWidth(1.5f)
                .setFillColor(color)
                .setFillOpacity(FILL_OPACITY);
    }

    private static int resolveAreaColor(ClaimedChunkData claim, UUID playerUUID) {
        if (claim.ownerUUID.equals(playerUUID)) {
            return COLOR_OWN;
        }
        Party localParty = ClientPartyCache.getPartyByPlayer(playerUUID);
        if (localParty != null && localParty.isMember(claim.ownerUUID)) {
            return COLOR_PARTY;
        }
        return COLOR_OTHER;
    }

    private static int resolveTextColor(ClaimedChunkData claim) {
        Party ownerParty = ClientPartyCache.getPartyByPlayer(claim.ownerUUID);
        if (ownerParty != null) {
            return ownerParty.getColor() & 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    private static String buildTitle(ClaimedChunkData claim, boolean allForceLoaded) {
        StringBuilder sb = new StringBuilder();
        sb.append(claim.ownerName);
        if (!claim.partyName.isEmpty()) {
            sb.append(" [").append(claim.partyName).append("]");
        }
        if (allForceLoaded) {
            sb.append(" (Force Loaded)");
        }
        return sb.toString();
    }
}
