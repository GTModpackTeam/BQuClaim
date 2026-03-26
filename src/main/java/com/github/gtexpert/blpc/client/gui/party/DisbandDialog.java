package com.github.gtexpert.blpc.client.gui.party;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

public class DisbandDialog {

    public static final String PANEL_ID = "blpc.party.dialog.disband";
    private static final int W = 220;
    private static final int H = 70;

    public static ModularPanel build(ModularPanel parentPanel) {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        panel.child(IKey.lang("blpc.party.disband_confirm_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 6))
                .child(IKey.lang("blpc.party.disband_confirm_msg").color(0xFFAAAAAA).shadow(true)
                        .asWidget().pos(8, 18))
                .child(new ButtonWidget<>().size(80, 16).pos(10, 48)
                        .overlay(IKey.lang("blpc.party.disband_yes"))
                        .onMousePressed(btn -> {
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.disband());
                            // Immediately clear client cache for instant feedback
                            UUID playerId = Minecraft.getMinecraft().player.getUniqueID();
                            ClientPartyCache.setLocalBQuLinked(playerId, false);
                            ClientPartyCache.clear();
                            panel.closeIfOpen();
                            parentPanel.closeIfOpen();
                            return true;
                        }))
                .child(new ButtonWidget<>().size(80, 16).pos(130, 48)
                        .overlay(IKey.lang("blpc.party.disband_no"))
                        .onMousePressed(btn -> {
                            panel.closeIfOpen();
                            return true;
                        }));

        return panel;
    }
}
