package com.github.gtexpert.blpc.client.gui.party;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.Party;

public class SettingsPanel {

    public static final String PANEL_ID = "blpc.party.settings";
    private static final int W = 220;
    private static final int H = 110;

    public static ModularPanel build(Party party) {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        panel.child(IKey.lang("blpc.party.settings_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8));
        panel.child(ButtonWidget.panelCloseButton());

        String fakePlayerLabel = party.allowsFakePlayers() ? "blpc.party.fakeplayer_on" : "blpc.party.fakeplayer_off";
        panel.child(new ButtonWidget<>().size(W - 16, 18).pos(8, 30)
                .overlay(IKey.lang(fakePlayerLabel))
                .onMousePressed(btn -> {
                    ModNetwork.INSTANCE.sendToServer(MessagePartyAction.toggleFakePlayers());
                    party.setAllowFakePlayers(!party.allowsFakePlayers());
                    reopenSelf(panel, party);
                    return true;
                }));

        String explosionLabel = party.protectsExplosions() ? "blpc.party.explosion_on" : "blpc.party.explosion_off";
        panel.child(new ButtonWidget<>().size(W - 16, 18).pos(8, 54)
                .overlay(IKey.lang(explosionLabel))
                .onMousePressed(btn -> {
                    ModNetwork.INSTANCE.sendToServer(MessagePartyAction.toggleExplosionProtection());
                    party.setProtectExplosions(!party.protectsExplosions());
                    reopenSelf(panel, party);
                    return true;
                }));

        return panel;
    }

    private static void reopenSelf(ModularPanel current, Party party) {
        ModularPanel parent = current.getScreen().getMainPanel();
        current.closeIfOpen();
        IPanelHandler.simple(parent, (pp, player) -> build(party), true).openPanel();
    }
}
