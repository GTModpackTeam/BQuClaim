package com.github.gtexpert.blpc.client.gui;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.RelationType;
import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.chunk.ClientClaimCache;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/**
 * Draws a brief on-screen indicator just above the food/stamina bar whenever the local player
 * crosses into a claimed chunk, so PvP fights always make it clear whether either side currently
 * has claim protection. Gated by {@link ModConfig.FairPlay#showProtectionStatusHud} — this is a
 * client-side display preference, evaluated purely from data already synced to
 * {@link ClientClaimCache}/{@link ClientPartyCache}.
 */
@SideOnly(Side.CLIENT)
public final class ProtectionStatusHud {

    /** Vanilla's food/stamina bar starts at {@code height - 39} (GuiIngameForge.left_height/right_height). */
    private static final int BOTTOM_MARGIN = 50;
    private static final int DISPLAY_TICKS = 100; // 5 seconds at 20 ticks/sec

    private long lastChunkKey = Long.MIN_VALUE;
    private int hideAtTick = -1;
    private String cachedText;
    private int cachedColor;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ModConfig.fairPlay.showProtectionStatusHud) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        long chunkKey = pack(mc.player.chunkCoordX, mc.player.chunkCoordZ);
        if (chunkKey != lastChunkKey) {
            lastChunkKey = chunkKey;
            onChunkChanged(mc);
        }

        if (hideAtTick < 0 || mc.player.ticksExisted > hideAtTick) return;

        ScaledResolution resolution = event.getResolution();
        FontRenderer font = mc.fontRenderer;
        int x = (resolution.getScaledWidth() - font.getStringWidth(cachedText)) / 2;
        font.drawStringWithShadow(cachedText, x, resolution.getScaledHeight() - BOTTOM_MARGIN, cachedColor);
    }

    /** Re-arms the 5-second display window whenever the player enters a newly-claimed chunk. */
    private void onChunkChanged(Minecraft mc) {
        ClaimedChunkData claim = ClientClaimCache.get(mc.player.chunkCoordX, mc.player.chunkCoordZ);
        if (claim == null) {
            hideAtTick = -1;
            return;
        }

        RelationType relation = resolveRelation(claim, mc.player.getUniqueID());
        cachedText = I18n.format("blpc.hud.protected_area",
                claim.partyName.isEmpty() ? claim.ownerName : claim.partyName);
        cachedColor = colorFor(relation);
        hideAtTick = mc.player.ticksExisted + DISPLAY_TICKS;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static RelationType resolveRelation(ClaimedChunkData claim, UUID localPlayerId) {
        if (claim.ownerUUID.equals(localPlayerId)) return RelationType.MEMBER;

        Party ownerParty = ClientPartyCache.getPartyByPlayer(claim.ownerUUID);
        if (ownerParty == null) return RelationType.NONE;
        if (ownerParty.isMember(localPlayerId)) return RelationType.MEMBER;

        Party localParty = ClientPartyCache.getPartyByPlayer(localPlayerId);
        if (localParty == null) return RelationType.NONE;
        if (ownerParty.isAlly(localParty.getPartyId())) return RelationType.ALLY;
        if (ownerParty.isEnemy(localParty.getPartyId())) return RelationType.ENEMY;
        return RelationType.NONE;
    }

    private static int colorFor(RelationType relation) {
        return switch (relation) {
            case MEMBER -> GuiColors.GREEN;
            case ALLY -> GuiColors.GOLD;
            case ENEMY -> GuiColors.RED;
            case NONE -> GuiColors.GRAY;
        };
    }
}
