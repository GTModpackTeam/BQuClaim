package com.github.gtexpert.blpc.client.gui.party;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.Party;

public class CreatePanel {

    public static final String PANEL_ID = "blpc.party.create";
    private static final int W = 220;
    private static final int H = 80;

    public static ModularPanel build() {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        TextFieldWidget nameField = new TextFieldWidget();
        nameField.size(140, 14).pos(8, 28);
        nameField.setText(IKey.lang(Party.DEFAULT_NAME_KEY).get());

        panel.child(IKey.lang("blpc.party.create_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8))
                .child(nameField)
                .child(new ButtonWidget<>().size(50, 14).pos(154, 28)
                        .overlay(IKey.lang("blpc.party.create"))
                        .onMousePressed(btn -> {
                            String name = nameField.getText().trim();
                            if (!name.isEmpty()) {
                                ModNetwork.INSTANCE.sendToServer(MessagePartyAction.create(name));
                            }
                            panel.closeIfOpen();
                            return true;
                        }))
                .child(ButtonWidget.panelCloseButton());

        return panel;
    }
}
