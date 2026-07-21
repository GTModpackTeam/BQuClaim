package com.github.gtexpert.blpc.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Semantic colors for the party panels and chunk map. Fixed light-context values —
 * BLPC ships a single light look, so colors are defined here in Java rather than
 * through a ModularUI theme indirection.
 * <p>
 * The {@code int} values are the single source of truth (private); consumers read
 * them through the accessor methods so a value change here propagates everywhere.
 */
@SideOnly(Side.CLIENT)
public final class BLPCColors {

    /** Primary text on the light panel background (titles, plain labels). */
    private static final int TEXT = 0xFF000000;
    /** Neutral text rendered on top of ModularUI's gray buttons (white, MC convention). */
    private static final int BUTTON_TEXT = 0xFFFFFFFF;
    // Role/accent colors render on gray buttons, so they use the bright vanilla palette
    // (readable with a shadow) rather than the dark light-panel tones.
    /** OWNER role / header accent (bright gold). */
    private static final int OWNER = GuiColors.GOLD;
    /** ADMIN role / active accent (bright green). */
    private static final int ADMIN = GuiColors.GREEN;
    /** Warning / limit exceeded (bright red). */
    private static final int WARNING = GuiColors.RED;
    /** Muted sub-text. */
    private static final int SUBTEXT = 0xFF555555;
    /** Inactive / non-member rows. */
    private static final int INACTIVE = 0xFF888888;
    /** Section divider line (alpha-blended). */
    private static final int DIVIDER = 0x40000000;
    /** Chunk-map backdrop tint (kept dark for map readability). */
    private static final int MAP_BACKGROUND = 0xE01C1C22;
    /** Chunk-map border accent. */
    private static final int MAP_BORDER = 0xFFFFFFFF;
    /** Map tile shown while a chunk's terrain is still loading. */
    private static final int MAP_UNLOADED = 0xFF222222;
    /** Claim overlay — owned by the local player (translucent green). */
    private static final int CLAIM_OWN = 0x5500FF00;
    /** Claim overlay — owned by a party member (translucent cyan). */
    private static final int CLAIM_PARTY = 0x5500FFFF;
    /** Claim overlay — owned by someone else (translucent red). */
    private static final int CLAIM_OTHER = 0x55FF0000;
    /** Force-load hatching overlay (translucent red). */
    private static final int CLAIM_HATCHING = 0xAAFF0000;
    /** Map hover/drag-selection overlay (translucent white — same cursor FTB's chunk map uses). */
    private static final int MAP_SELECTION = 0x21FFFFFF;

    private BLPCColors() {}

    /** Primary text on the light panel background (titles, plain labels). */
    public static int text() {
        return TEXT;
    }

    /** Neutral text rendered on a gray button (white); pair with {@link #buttonTextShadow()}. */
    public static int buttonText() {
        return BUTTON_TEXT;
    }

    /** Buttons use white text on gray, so a drop shadow improves readability. */
    public static boolean buttonTextShadow() {
        return true;
    }

    /** OWNER role / header accent. */
    public static int owner() {
        return OWNER;
    }

    /** ADMIN role / active accent. */
    public static int admin() {
        return ADMIN;
    }

    /** Warning / limit exceeded. */
    public static int warning() {
        return WARNING;
    }

    /** Muted sub-text. */
    public static int subtext() {
        return SUBTEXT;
    }

    /** Inactive / non-member rows. */
    public static int inactive() {
        return INACTIVE;
    }

    /** Section divider line. */
    public static int divider() {
        return DIVIDER;
    }

    /** Backdrop tint for the chunk-map panel. */
    public static int mapBackground() {
        return MAP_BACKGROUND;
    }

    /** Border accent for the chunk-map panel. */
    public static int mapBorder() {
        return MAP_BORDER;
    }

    /** Fill shown for a chunk whose terrain has not rendered yet. */
    public static int mapUnloaded() {
        return MAP_UNLOADED;
    }

    /** Claim overlay color for the local player's own chunks. */
    public static int claimOwn() {
        return CLAIM_OWN;
    }

    /** Claim overlay color for party-mate chunks. */
    public static int claimParty() {
        return CLAIM_PARTY;
    }

    /** Claim overlay color for other players' chunks. */
    public static int claimOther() {
        return CLAIM_OTHER;
    }

    /** Force-load hatching overlay color. */
    public static int claimHatching() {
        return CLAIM_HATCHING;
    }

    /** Map hover/drag-selection overlay color. */
    public static int mapSelection() {
        return MAP_SELECTION;
    }

    /** Border drawn between differently-owned claims (same accent as {@link #mapBorder()}). */
    public static int claimBorder() {
        return MAP_BORDER;
    }

    /** Composes an opaque ARGB from a party's stored 24-bit RGB color. */
    public static int partyArgb(int partyRgb) {
        return 0xFF000000 | (partyRgb & 0xFFFFFF);
    }

    /** Whether title text should cast a shadow (off for the light look). */
    public static boolean textShadow() {
        return false;
    }
}
