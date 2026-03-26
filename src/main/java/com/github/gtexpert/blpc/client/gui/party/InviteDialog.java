package com.github.gtexpert.blpc.client.gui.party;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;

public class InviteDialog {

    public static final String PANEL_ID = "blpc.party.dialog.invite";
    private static final int W = 180;
    private static final int H = 60;

    public static ModularPanel build() {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        TextFieldWidget inviteField = new TextFieldWidget();
        inviteField.size(120, 14).pos(8, 24);

        panel.child(IKey.lang("blpc.party.invite_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 6))
                .child(inviteField)
                .child(new ButtonWidget<>().size(40, 14).pos(132, 24)
                        .overlay(IKey.lang("blpc.party.send"))
                        .onMousePressed(btn -> {
                            String username = inviteField.getText().trim();
                            if (!username.isEmpty()) {
                                ModNetwork.INSTANCE.sendToServer(
                                        MessagePartyAction.invite(username));
                            }
                            panel.closeIfOpen();
                            return true;
                        }))
                .child(ButtonWidget.panelCloseButton());

        return panel;
    }
}
