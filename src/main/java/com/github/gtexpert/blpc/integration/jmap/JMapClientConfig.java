package com.github.gtexpert.blpc.integration.jmap;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import journeymap.api.v2.common.option.BooleanOption;
import journeymap.api.v2.common.option.IntegerOption;
import journeymap.api.v2.common.option.Option;

/**
 * Client-side JourneyMap integration config backed by JourneyMap's v2
 * {@link journeymap.api.v2.common.option.OptionsRegistry}. Values are read directly from
 * the registered {@link Option} instances, so changes made through JourneyMap's Addon Options
 * screen take effect immediately without a manual sync step.
 * <p>
 * Before {@link #init} is called (i.e. before JMapPlugin initializes), the getters
 * return sensible defaults so early callers never NPE.
 */
@SideOnly(Side.CLIENT)
public final class JMapClientConfig {

    private static BooleanOption showClaimOverlays;
    private static BooleanOption waypointSharingEnabled;
    private static IntegerOption waypointSyncInterval;

    private JMapClientConfig() {}

    static void init(BooleanOption overlays, BooleanOption waypoints, IntegerOption syncInterval) {
        showClaimOverlays = overlays;
        waypointSharingEnabled = waypoints;
        waypointSyncInterval = syncInterval;
    }

    public static boolean isShowClaimOverlays() {
        return safeGet(showClaimOverlays, true);
    }

    public static boolean isWaypointSharingEnabled() {
        return safeGet(waypointSharingEnabled, true);
    }

    public static int getWaypointSyncInterval() {
        return safeGet(waypointSyncInterval, 100);
    }

    @SuppressWarnings("unchecked")
    private static <T> T safeGet(Option<T> option, T defaultValue) {
        if (option == null) return defaultValue;
        try {
            T val = option.get();
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
