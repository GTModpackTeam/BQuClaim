package com.github.gtexpert.blpc.integration.jmap;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side, runtime-only JourneyMap integration toggles, edited from
 * {@link JourneyMapAddonPanel}. Deliberately holds no JourneyMap API types so it
 * can be read from {@link BLPCJourneyMapPlugin} without widening its dependency
 * surface. Not persisted — resets to defaults each session, matching the previous
 * minimap-toggle behavior.
 */
@SideOnly(Side.CLIENT)
public final class JMapClientConfig {

    private static boolean showClaimOverlays = true;

    private JMapClientConfig() {}

    /** Whether BLPC claim regions are drawn on JourneyMap. */
    public static boolean isShowClaimOverlays() {
        return showClaimOverlays;
    }

    public static void setShowClaimOverlays(boolean value) {
        showClaimOverlays = value;
    }
}
