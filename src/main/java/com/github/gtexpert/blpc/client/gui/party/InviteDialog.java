package com.github.gtexpert.blpc.client.gui.party;

import com.cleanroommc.modularui.widgets.Dialog;

import com.github.gtexpert.blpc.client.gui.party.widget.InputDialog;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;

/**
 * Player invite dialog (panel ID: {@value #PANEL_ID}).
 * <p>
 * Built using {@link InputDialog} template. Sends an invite request
 * with the entered player name.
 */
public class InviteDialog {

    public static final String PANEL_ID = "blpc.party.dialog.invite";

    /** Builds the invite dialog. */
    public static Dialog<Void> build() {
        return InputDialog.builder(PANEL_ID)
                .title("blpc.party.invite_title")
                .confirmLabel("blpc.party.send")
                .onSubmit(username -> ModNetwork.INSTANCE.sendToServer(MessagePartyAction.invite(username)))
                .build();
    }
}
