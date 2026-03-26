package com.github.gtexpert.blpc.client.gui.party;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;

public class LinkBQuDialog {

    public static final String PANEL_ID = "blpc.party.dialog.link_bqu";
    private static final int W = 240;
    private static final int H = 80;

    public static ModularPanel build(ModularPanel parentPanel) {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        panel.child(IKey.lang("blpc.party.link_bqu_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 6))
                .child(IKey.lang("blpc.party.link_bqu_msg").color(0xFFAAAAAA).shadow(true)
                        .asWidget().pos(8, 20))
                .child(new ButtonWidget<>().size(80, 16).pos(20, 56)
                        .overlay(IKey.lang("blpc.map.yes"))
                        .onMousePressed(btn -> {
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.toggleBQuLink(true));
                            panel.closeIfOpen();
                            parentPanel.closeIfOpen();
                            Minecraft.getMinecraft().addScheduledTask(PartyProviderRegistry::openNativeScreen);
                            return true;
                        }))
                .child(new ButtonWidget<>().size(80, 16).pos(140, 56)
                        .overlay(IKey.lang("blpc.map.no"))
                        .onMousePressed(btn -> {
                            panel.closeIfOpen();
                            return true;
                        }));

        return panel;
    }
}
