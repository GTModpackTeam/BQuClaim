package com.github.gtexpert.blpc.client.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.client.gui.party.widget.ConfirmDialog;
import com.github.gtexpert.blpc.client.map.AsyncMapRenderer;
import com.github.gtexpert.blpc.client.map.TextureCache;
import com.github.gtexpert.blpc.common.ModConfig;
import com.github.gtexpert.blpc.common.chunk.ClaimedChunkData;
import com.github.gtexpert.blpc.common.chunk.ClientCache;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.network.message.ClaimChunk;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

public class ChunkMapScreen extends CustomModularScreen {

    private static final int BTN_SIZE = 16;
    private static final int BTN_GAP = 2;
    // Max map draw area; ChunkMapWidget derives its per-chunk pixel size from this and its GRID.
    private static final int MAP_PX = 195;
    private static final int COUNTER_RIGHT = 4;
    private static final int COUNTER_BOTTOM_CLAIMED = 16;
    private static final int COUNTER_BOTTOM_LOADED = 4;

    private ChunkMapWidget mapWidget;
    private IPanelHandler confirmHandler;
    private IPanelHandler partyHandler;
    private int pendingConfirmAction;

    public ChunkMapScreen() {
        super(Tags.MODID);
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        mapWidget = new ChunkMapWidget();

        int blockW = MAP_PX + BTN_SIZE;
        int leftOff = -blockW / 2;

        return new ModularPanel("blpc.map")
                .fullScreenInvisible()
                .child(mapWidget.size(MAP_PX, MAP_PX)
                        .leftRel(0.5f, leftOff, 0f)
                        .verticalCenter()
                        .background(BLPCGuiTextures.MAP_BACKGROUND)
                        .overlay(BLPCGuiTextures.MAP_BORDER))
                .child(createToolButtons()
                        .leftRel(0.5f, leftOff + MAP_PX, 0f)
                        .verticalCenter())
                .child(createCounterText("blpc.map.claimed_chunks",
                        this::countMyClaims, this::maxClaims, COUNTER_BOTTOM_CLAIMED))
                .child(createCounterText("blpc.map.loaded_chunks",
                        this::countMyForceLoads, this::maxForceLoads, COUNTER_BOTTOM_LOADED));
    }

    private TextWidget<?> createCounterText(String langKey,
                                            IntSupplier counter, IntSupplier max, int bottom) {
        return new TextWidget<>(IKey.lang(langKey, () -> new Object[] { counter.getAsInt(), max.getAsInt() }))
                .color(() -> counter.getAsInt() >= max.getAsInt() ? GuiColors.RED : GuiColors.WHITE)
                .shadow(true).right(COUNTER_RIGHT).bottom(bottom);
    }

    private ParentWidget<?> createToolButtons() {
        int n = 5;
        int totalH = BTN_SIZE * n + BTN_GAP * (n - 1);
        return Flow.col()
                .size(BTN_SIZE, totalH)
                .childPadding(BTN_GAP)
                .child(createToolButton(BLPCGuiTextures.ICON_CLOSE, mb -> close(), "blpc.map.close"))
                .child(createToolButton(IKey.str("P"), mb -> openPartyScreen(), "blpc.map.party"))
                .child(createToolButton(BLPCGuiTextures.ICON_REFRESH, mb -> {
                    AsyncMapRenderer.clearCache();
                    TextureCache.clear();
                }, "blpc.map.redraw"))
                .child(createToolButton(BLPCGuiTextures.ICON_REMOVE, mb -> openConfirmDialog(1),
                        "blpc.map.unclaim_all", "blpc.map.help_claim", "blpc.map.help_unclaim"))
                .child(createToolButton(IKey.str("L"), mb -> openConfirmDialog(2),
                        "blpc.map.unload_all", "blpc.map.help_force", "blpc.map.help_drag"));
    }

    private ButtonWidget<?> createToolButton(IDrawable overlay, Consumer<Integer> action,
                                             String... tooltipKeys) {
        ButtonWidget<?> btn = new ButtonWidget<>();
        btn.size(BTN_SIZE, BTN_SIZE)
                .overlay(overlay)
                .onMousePressed(mb -> {
                    if (mb == 0) {
                        action.accept(mb);
                        return true;
                    }
                    return false;
                });
        if (tooltipKeys.length > 0) {
            btn.addTooltipLine(IKey.lang(tooltipKeys[0]));
        }
        if (tooltipKeys.length > 1) {
            btn.addTooltipLine(IKey.str(""));
        }
        for (int i = 1; i < tooltipKeys.length; i++) {
            btn.addTooltipLine(IKey.lang(tooltipKeys[i]).color(GuiColors.GRAY));
        }
        return btn;
    }

    private void openConfirmDialog(int action) {
        pendingConfirmAction = action;
        if (confirmHandler != null) {
            confirmHandler.deleteCachedPanel();
        } else {
            confirmHandler = IPanelHandler.simple(
                    getMainPanel(), (parentPanel, player) -> buildConfirmDialog(), true);
        }
        confirmHandler.openPanel();
    }

    private Dialog<Boolean> buildConfirmDialog() {
        boolean isUnclaim = pendingConfirmAction == 1;
        int action = pendingConfirmAction;
        return ConfirmDialog.builder("blpc.map.dialog.confirm")
                .title(isUnclaim ? "blpc.map.confirm_unclaim_title" : "blpc.map.confirm_unload_title")
                .message(isUnclaim ? "blpc.map.confirm_unclaim_msg" : "blpc.map.confirm_unload_msg")
                .onConfirm(() -> executeBulkAction(action))
                .closeParent(false)
                .build(getMainPanel());
    }

    private void executeBulkAction(int action) {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        int mode = (action == 1) ? ClaimChunk.MODE_UNCLAIM : ClaimChunk.MODE_TOGGLE_FORCE;
        for (ClaimedChunkData d : ClientCache.getAll()) {
            if (d.ownerUUID.equals(myId)) {
                if (action == 1 || d.isForceLoaded) {
                    ModNetwork.INSTANCE.sendToServer(new ClaimChunk(d.x, d.z, mode));
                }
            }
        }
    }

    private void openPartyScreen() {
        if (partyHandler != null) {
            partyHandler.deleteCachedPanel();
        }
        partyHandler = IPanelHandler.simple(getMainPanel(), (parentPanel, player) -> {
            // Pass the handler back into the party factory so that CreatePanel can
            // re-invoke it after a successful join — the factory re-runs and
            // returns MainPanel automatically.
            return Screens.partyMain(
                    Minecraft.getMinecraft().player.getUniqueID(), partyHandler);
        }, true);
        partyHandler.openPanel();
    }

    private int countMyClaims() {
        return countChunks(false);
    }

    private int countMyForceLoads() {
        return countChunks(true);
    }

    private int countChunks(boolean forceLoadedOnly) {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        Set<UUID> ids = getPartyMemberIds(myId);
        return (int) ClientCache.getAll().stream()
                .filter(d -> ids.contains(d.ownerUUID) && (!forceLoadedOnly || d.isForceLoaded))
                .count();
    }

    private int maxClaims() {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        Party party = ClientPartyCache.getPartyByPlayer(myId);
        if (party != null && ModConfig.claims.additiveLimits) {
            return party.sumClaimLimit(ModConfig.claims.maxClaimsPerPlayer);
        }
        return ModConfig.claims.maxClaimsPerPlayer;
    }

    private int maxForceLoads() {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        Party party = ClientPartyCache.getPartyByPlayer(myId);
        if (party != null && ModConfig.claims.additiveLimits) {
            return party.sumForceLoadLimit(ModConfig.claims.maxForceLoadsPerPlayer);
        }
        return ModConfig.claims.maxForceLoadsPerPlayer;
    }

    private static Set<UUID> getPartyMemberIds(UUID myId) {
        if (ModConfig.claims.additiveLimits) {
            Party party = ClientPartyCache.getPartyByPlayer(myId);
            if (party != null) {
                return new HashSet<>(party.getMemberUUIDs());
            }
        }
        return Collections.singleton(myId);
    }
}
