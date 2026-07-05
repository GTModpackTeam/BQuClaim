package com.github.gtexpert.blpc.client.gui.addons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

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

        @SuppressWarnings("unchecked")
        ListWidget<IWidget, ?> list = new ListWidget<>();
        list.widthRel(1f).heightRel(1f);
        list.crossAxisAlignment(Alignment.CrossAxis.START);

        List<AddonPanelRegistry.Entry> entries = AddonPanelRegistry.available();
        var widgets = new ArrayList<IWidget>();
        var searchNames = new ArrayList<String>();

        for (AddonPanelRegistry.Entry entry : entries) {
            ButtonWidget<?> btn = buildEntryButton(panel, entry, playerId);
            widgets.add(btn);
            searchNames.add(IKey.lang(entry.labelKey()).get().toLowerCase(Locale.ROOT));
            list.child(btn);
        }

        IWidget content = PartyWidgets.finalizeSearchableList(
                list, widgets, searchNames, "blpc.addons.empty");
        Flow wrapper = PartyWidgets.fillBelowHeader(
                Flow.column().child(content));
        panel.child(wrapper);
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
