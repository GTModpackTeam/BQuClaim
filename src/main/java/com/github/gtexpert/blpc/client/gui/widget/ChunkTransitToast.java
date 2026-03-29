package com.github.gtexpert.blpc.client.gui.widget;

import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.party.RelationType;

/**
 * Reusable toast notification template for chunk territory events.
 * <p>
 * Uses the Builder pattern for flexible configuration.
 * The vanilla {@link IToast} API renders toasts in the top-right corner.
 *
 * <pre>
 * {@code
 * ChunkTransitToast.builder()
 *     .fromTransit(RelationType.ENEMY, true, "PlayerName")
 *     .build();
 * }
 * </pre>
 */
public class ChunkTransitToast implements IToast {

    private final String title;
    private final int color;
    private long firstDrawTime = -1L;

    private ChunkTransitToast(String title, int color) {
        this.title = title;
        this.color = color;
    }

    @Override
    public Visibility draw(GuiToast toastGui, long delta) {
        if (firstDrawTime < 0) {
            firstDrawTime = delta;
        }

        toastGui.getMinecraft().getTextureManager()
                .bindTexture(TEXTURE_TOASTS);
        GlStateManager.color(1.0f, 1.0f, 1.0f);
        toastGui.drawTexturedModalRect(0, 0, 0, 0, 160, 32);

        toastGui.getMinecraft().fontRenderer.drawString(
                title, 7, 12, color);

        long elapsed = delta - firstDrawTime;
        return elapsed >= ModConfig.transitToastDuration ? Visibility.HIDE : Visibility.SHOW;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String titleKey = "";
        private Object[] titleArgs = {};
        private int color = GuiColors.WHITE;

        /** Sets the title lang key and arguments directly. */
        public Builder title(String langKey, Object... args) {
            this.titleKey = langKey;
            this.titleArgs = args;
            return this;
        }

        /** Sets the color for the toast text. */
        public Builder color(int argb) {
            this.color = argb;
            return this;
        }

        /**
         * Auto-configures title and color based on relation type and direction.
         *
         * @param relation   relationship between the transiting player and the chunk owner
         * @param entered    true if the player entered the chunk, false if they left
         * @param playerName display name of the transiting player
         */
        public Builder fromTransit(RelationType relation, boolean entered, String playerName) {
            String direction = entered ? "enter" : "leave";
            switch (relation) {
                case MEMBER -> {
                    this.titleKey = "blpc.transit.member." + direction;
                    this.color = GuiColors.GREEN;
                }
                case ALLY -> {
                    this.titleKey = "blpc.transit.ally." + direction;
                    this.color = GuiColors.GOLD;
                }
                case ENEMY -> {
                    this.titleKey = "blpc.transit.enemy." + direction;
                    this.color = GuiColors.RED;
                }
                case NONE -> this.titleKey = "";
            }
            this.titleArgs = new Object[] { playerName };
            return this;
        }

        public ChunkTransitToast build() {
            String resolved = titleKey.isEmpty() ? "" : I18n.format(titleKey, titleArgs);
            return new ChunkTransitToast(resolved, color);
        }
    }
}
