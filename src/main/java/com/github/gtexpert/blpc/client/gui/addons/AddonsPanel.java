package com.github.gtexpert.blpc.client.gui.addons;

import java.util.UUID;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.client.gui.party.PartyWidgets;

/**
 * Addons hub (panel ID: {@value #PANEL_ID}). Lists one row button per available
 * {@link AddonPanelRegistry.Entry}; each opens that mod's own settings panel.
 * Reached from the party main menu's <em>Addons</em> entry.
 */
public final class AddonsPanel {

    public static final String PANEL_ID = "blpc.party.addons";

    private AddonsPanel() {}

    public static ModularPanel build(UUID playerId) {
        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
        PartyWidgets.addHeader(panel, "blpc.addons.title");

        @SuppressWarnings("rawtypes")
        ListWidget list = new ListWidget();

        var entries = AddonPanelRegistry.available();
        if (entries.isEmpty()) {
            list.child(PartyWidgets.emptyStateRow("blpc.addons.empty"));
        } else {
            for (AddonPanelRegistry.Entry entry : entries) {
                list.child(buildEntryButton(panel, entry, playerId));
            }
        }

        PartyWidgets.addList(panel, list);
        return panel;
    }

    private static ButtonWidget<?> buildEntryButton(ModularPanel panel,
                                                    AddonPanelRegistry.Entry entry, UUID playerId) {
        // One handler per entry, created once with the panel — IPanelHandler.simple
        // registers into panel.clientSubPanels (no removal API), and this panel is a
        // fresh instance on every open, so there is nothing to leak across opens.
        IPanelHandler handler = IPanelHandler.simple(
                panel, (pp, player) -> entry.createPanel(playerId), true);
        ButtonWidget<?> btn = PartyWidgets.dialogButton(
                IKey.lang(entry.labelKey()).alignment(Alignment.CenterLeft), handler)
                .widthRel(1f).height(PartyWidgets.BTN_H)
                .padding(PartyWidgets.ROW_INDENT, 0, 0, 0);
        if (entry.tooltipKey() != null) {
            btn.addTooltipLine(IKey.lang(entry.tooltipKey()));
        }
        return btn;
    }
}
