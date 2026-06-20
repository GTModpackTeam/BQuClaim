package com.github.gtexpert.blpc.client.gui;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;

/**
 * Central holder for reusable BLPC {@link IDrawable}s — mirrors ModularUI's own
 * {@code GuiTextures} pattern (one place where every shared drawable lives).
 * <p>
 * Drawables here are stateless and intentionally shared across widgets: a
 * {@link Rectangle} only reads its configured fields when drawn, so a single
 * instance can back many widgets. Wrap with {@code .asWidget()} when a standalone
 * widget is needed, or pass directly to {@code background(...)} / {@code overlay(...)}.
 * <p>
 * The {@code ICON_*} constants reuse ModularUI's built-in {@link GuiTextures} icon
 * atlas (no custom art) so BLPC buttons stay visually consistent with the framework.
 */
@SideOnly(Side.CLIENT)
public final class BLPCGuiTextures {

    /** Thin section divider line (1px high when wrapped with {@code .asWidget().height(1)}). */
    public static final IDrawable DIVIDER = new Rectangle().color(GuiColors.DIVIDER);
    /** Chunk-map panel backdrop (dark tint). */
    public static final IDrawable MAP_BACKGROUND = new Rectangle().color(BLPCColors.mapBackground());
    /** Chunk-map panel 1px hollow border. */
    public static final IDrawable MAP_BORDER = new Rectangle().color(BLPCColors.mapBorder()).hollow(1);

    /** Close / exit (chunk map "X" button). */
    public static final IDrawable ICON_CLOSE = GuiTextures.CLOSE;
    /** Refresh / redraw (chunk map "R" button). */
    public static final IDrawable ICON_REFRESH = GuiTextures.REFRESH;
    /** Remove / delete (chunk map "unclaim all" button). */
    public static final IDrawable ICON_REMOVE = GuiTextures.REMOVE;

    private BLPCGuiTextures() {}
}
