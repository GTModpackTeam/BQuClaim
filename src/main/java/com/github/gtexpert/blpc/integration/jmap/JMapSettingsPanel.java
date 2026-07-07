package com.github.gtexpert.blpc.integration.jmap;

import java.util.UUID;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.client.gui.party.PartyWidgets;

/**
 * JourneyMap settings panel (panel ID: {@value #PANEL_ID}). Reached from the party
 * menu's Addons hub. Hosts the client-side claim-overlay visibility toggle and the
 * team waypoint sharing toggle.
 */
@SideOnly(Side.CLIENT)
public final class JMapSettingsPanel {

    public static final String PANEL_ID = "blpc.party.addons.journeymap";

    private JMapSettingsPanel() {}

    public static ModularPanel build(UUID playerId) {
        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
        PartyWidgets.addHeader(panel, "blpc.addons.journeymap.title");

        @SuppressWarnings("rawtypes")
        ListWidget list = new ListWidget();

        list.child(PartyWidgets.toggleButton(
                new BoolValue.Dynamic(
                        JMapClientConfig::isShowClaimOverlays,
                        val -> {
                            JMapClientConfig.setShowClaimOverlays(val);
                            JMapPlugin.refreshFromSettings();
                        }),
                "blpc.addons.journeymap.overlays_off", "blpc.addons.journeymap.overlays_on")
                .addTooltipLine(IKey.lang("blpc.addons.journeymap.overlays_tooltip")));

        list.child(PartyWidgets.toggleButton(
                new BoolValue.Dynamic(
                        JMapClientConfig::isWaypointSharingEnabled,
                        val -> {
                            JMapClientConfig.setWaypointSharingEnabled(val);
                            JMapWaypointSyncHandler.refreshFromSettings();
                        }),
                "blpc.addons.journeymap.waypoints_off", "blpc.addons.journeymap.waypoints_on")
                .addTooltipLine(IKey.lang("blpc.addons.journeymap.waypoints_tooltip")));

        PartyWidgets.addList(panel, list);
        return panel;
    }
}
