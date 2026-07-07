package com.github.gtexpert.blpc.integration.jmap;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side, runtime-only JourneyMap integration toggles, edited from
 * {@link JMapSettingsPanel}. Deliberately holds no JourneyMap API types so it
 * can be read from {@link JMapPlugin} without widening its dependency
 * surface. Not persisted — resets to defaults each session, matching the previous
 * minimap-toggle behavior.
 */
@SideOnly(Side.CLIENT)
public final class JMapClientConfig {

    private static boolean showClaimOverlays = true;
    private static boolean waypointSharingEnabled = true;

    private JMapClientConfig() {}

    /** Whether BLPC claim regions are drawn on JourneyMap. */
    public static boolean isShowClaimOverlays() {
        return showClaimOverlays;
    }

    public static void setShowClaimOverlays(boolean value) {
        showClaimOverlays = value;
    }

    /**
     * Whether party-shared waypoints are synced. Only the party OWNER's edits are ever sent
     * (see {@code WaypointAction} javadoc) — this toggle just lets any member opt out of
     * receiving/mirroring them locally.
     */
    public static boolean isWaypointSharingEnabled() {
        return waypointSharingEnabled;
    }

    public static void setWaypointSharingEnabled(boolean value) {
        waypointSharingEnabled = value;
    }
}
