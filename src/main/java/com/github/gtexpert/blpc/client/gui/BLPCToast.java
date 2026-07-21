package com.github.gtexpert.blpc.client.gui;

import java.util.UUID;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.api.party.RelationType;
import com.github.gtexpert.blpc.common.ModConfig;

/**
 * Generic reusable toast notification for BLPC events.
 * <p>
 * Uses the Builder pattern for flexible configuration.
 * The vanilla {@link IToast} API renders toasts in the top-right corner.
 *
 * <pre>
 * {@code
 * BLPCToast.builder()
 *     .fromTransit(RelationType.ENEMY, true, "PlayerName", playerUUID, "", false)
 *     .build();
 * }
 * </pre>
 */
@SideOnly(Side.CLIENT)
public class BLPCToast implements IToast {

    private static final int FACE_SIZE = 16;
    private static final int FACE_MARGIN = 6;

    private final String title;
    private final int color;
    private final UUID headUUID;
    private long firstDrawTime = -1L;

    private BLPCToast(String title, int color, UUID headUUID) {
        this.title = title;
        this.color = color;
        this.headUUID = headUUID;
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

        int textX = 7;
        if (headUUID != null) {
            drawHead(toastGui, headUUID);
            textX = FACE_MARGIN + FACE_SIZE + 4;
        }
        toastGui.getMinecraft().fontRenderer.drawString(
                title, textX, 12, color);

        long elapsed = delta - firstDrawTime;
        return elapsed >= ModConfig.Defaults.transitToastDuration ? Visibility.HIDE : Visibility.SHOW;
    }

    /**
     * Same approach vanilla uses for tab-list player heads
     * ({@code GuiPlayerTabOverlay.drawPlayerListEntry}): {@link Gui#drawScaledCustomSizeModalRect}
     * against the 64x64 skin texture, base face layer then the hat overlay on top.
     */
    private static void drawHead(GuiToast toastGui, UUID playerUUID) {
        ResourceLocation skin = PlayerFaceDrawable.resolveSkin(playerUUID);
        toastGui.getMinecraft().getTextureManager().bindTexture(skin);
        GlStateManager.color(1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(FACE_MARGIN, FACE_MARGIN, 8f, 8f, 8, 8, FACE_SIZE, FACE_SIZE, 64f, 64f);
        Gui.drawScaledCustomSizeModalRect(FACE_MARGIN, FACE_MARGIN, 40f, 8f, 8, 8, FACE_SIZE, FACE_SIZE, 64f, 64f);
        GlStateManager.disableBlend();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String titleKey = "";
        private Object[] titleArgs = {};
        private int color = GuiColors.WHITE;
        private UUID headUUID;

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

        /** Shows the given player's head to the left of the title text. */
        public Builder head(UUID playerUUID) {
            this.headUUID = playerUUID;
            return this;
        }

        /**
         * Auto-configures title and color based on relation type and direction. When {@code self}
         * is true (this toast is for the transiting player themself, not a third-party party
         * member watching them come and go), second-person wording is used instead — otherwise
         * "YourName returned home" reads like a report about someone else.
         *
         * @param relation   relationship between the transiting player and the chunk owner
         * @param entered    true if the player entered the chunk, false if they left
         * @param playerName display name of the transiting player (member/ally/enemy toasts)
         * @param playerUUID UUID of the transiting player, for the head icon (member/ally/enemy
         *                   toasts only); may be {@code null}
         * @param ownerName  owner/party display name of the claim (NONE toasts only)
         * @param self       true if the recipient is the transiting player themself
         */
        public Builder fromTransit(RelationType relation, boolean entered, String playerName, UUID playerUUID,
                                   String ownerName, boolean self) {
            String direction = entered ? "enter" : "leave";
            String selfSuffix = self ? ".self" : "";
            switch (relation) {
                case MEMBER -> {
                    this.titleKey = "blpc.transit.member." + direction + selfSuffix;
                    this.color = GuiColors.GREEN;
                    this.titleArgs = self ? new Object[] {} : new Object[] { playerName };
                    this.headUUID = self ? null : playerUUID;
                }
                case ALLY -> {
                    this.titleKey = "blpc.transit.ally." + direction + selfSuffix;
                    this.color = GuiColors.GOLD;
                    this.titleArgs = self ? new Object[] { ownerName } : new Object[] { playerName };
                    this.headUUID = self ? null : playerUUID;
                }
                case ENEMY -> {
                    this.titleKey = "blpc.transit.enemy." + direction + selfSuffix;
                    this.color = GuiColors.RED;
                    this.titleArgs = self ? new Object[] { ownerName } : new Object[] { playerName };
                    this.headUUID = self ? null : playerUUID;
                }
                case NONE -> {
                    if (ownerName == null || ownerName.isEmpty()) {
                        this.titleKey = "";
                    } else {
                        this.titleKey = entered ? "blpc.transit.none.enter" : "blpc.transit.none.leave";
                        this.titleArgs = new Object[] { ownerName };
                    }
                    this.color = GuiColors.GRAY;
                }
            }
            return this;
        }

        /**
         * Configures the toast for a party event notification.
         *
         * @param eventType  event type string (e.g. "MEMBER_JOINED")
         * @param playerName name of the relevant player
         * @param partyName  party name or role name (context-dependent)
         */
        public Builder fromPartyEvent(String eventType, String playerName, String partyName) {
            switch (eventType) {
                case "MEMBER_JOINED" -> {
                    this.titleKey = "blpc.toast.member_joined";
                    this.titleArgs = new Object[] { playerName };
                    this.color = GuiColors.GREEN;
                }
                case "MEMBER_LEFT" -> {
                    this.titleKey = "blpc.toast.member_left";
                    this.titleArgs = new Object[] { playerName };
                    this.color = GuiColors.GRAY;
                }
                case "KICKED" -> {
                    this.titleKey = "blpc.toast.kicked";
                    this.titleArgs = new Object[] { playerName };
                    this.color = GuiColors.GRAY;
                }
                case "DISBANDED" -> {
                    this.titleKey = "blpc.toast.disbanded";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.RED;
                }
                case "INVITE_RECEIVED" -> {
                    this.titleKey = "blpc.toast.invite_received";
                    this.titleArgs = new Object[] { playerName, partyName };
                    this.color = GuiColors.GOLD;
                }
                case "OWNER_TRANSFERRED" -> {
                    this.titleKey = "blpc.toast.owner_transferred";
                    this.titleArgs = new Object[] { playerName };
                    this.color = GuiColors.GOLD;
                }
                case "ROLE_CHANGED" -> {
                    this.titleKey = "blpc.toast.role_changed";
                    this.titleArgs = new Object[] { partyName };
                    this.color = GuiColors.GREEN;
                }
                case "BQU_LINKED" -> {
                    this.titleKey = "blpc.toast.bqu_linked";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.WHITE;
                }
                case "BQU_UNLINKED" -> {
                    this.titleKey = "blpc.toast.bqu_unlinked";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.WHITE;
                }
                case "PARTY_FULL" -> {
                    this.titleKey = "blpc.toast.party_full";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.RED;
                }
                case "JOIN_FAILED" -> {
                    this.titleKey = "blpc.toast.join_failed";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.RED;
                }
                default -> this.titleKey = "";
            }
            return this;
        }

        /**
         * Configures the toast for a claim failure notification.
         *
         * @param reason  failure reason ("CLAIM_LIMIT", "FORCELOAD_LIMIT", "NO_PARTY", or "DIMENSION_BLOCKED")
         * @param current current count (unused for "NO_PARTY")
         * @param max     maximum allowed count (unused for "NO_PARTY")
         */
        public Builder fromClaimFailed(String reason, int current, int max) {
            switch (reason) {
                case "CLAIM_LIMIT" -> {
                    this.titleKey = "blpc.toast.claim_limit";
                    this.titleArgs = new Object[] { current, max };
                    this.color = GuiColors.RED;
                }
                case "FORCELOAD_LIMIT" -> {
                    this.titleKey = "blpc.toast.forceload_limit";
                    this.titleArgs = new Object[] { current, max };
                    this.color = GuiColors.RED;
                }
                case "NO_PARTY" -> {
                    this.titleKey = "blpc.toast.no_party";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.RED;
                }
                case "DIMENSION_BLOCKED" -> {
                    this.titleKey = "blpc.toast.dimension_blocked";
                    this.titleArgs = new Object[] {};
                    this.color = GuiColors.RED;
                }
                default -> this.titleKey = "";
            }
            return this;
        }

        public BLPCToast build() {
            String resolved = titleKey.isEmpty() ? "" : I18n.format(titleKey, titleArgs);
            return new BLPCToast(resolved, color, headUUID);
        }
    }
}
