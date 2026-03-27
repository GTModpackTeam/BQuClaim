package com.github.gtexpert.blpc.client.gui.party;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.cleanroommc.modularui.screen.ModularPanel;

import com.github.gtexpert.blpc.client.gui.party.widget.PlayerListPanel;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.Party;

/**
 * Ally management panel (panel ID: {@value #PANEL_ID}).
 * <p>
 * FTB Utilities style: shows ALL online players except party members.
 * Allies shown in yellow, non-allies in gray. Click to toggle status.
 */
public class AlliesPanel {

    public static final String PANEL_ID = "blpc.party.allies";

    public static ModularPanel build(Party party) {
        Set<UUID> memberIds = new HashSet<>(party.getMembers().keySet());

        return PlayerListPanel.builder(PANEL_ID)
                .title("blpc.party.allies_title")
                .activeSet(party.getAllies())
                .activeColor(0xFFFFFF55)
                .inactiveColor(0xFFAAAAAA)
                .excludeUUIDs(memberIds)
                .onActivate(name -> ModNetwork.INSTANCE.sendToServer(MessagePartyAction.addAlly(name)))
                .onDeactivate(name -> ModNetwork.INSTANCE.sendToServer(MessagePartyAction.removeAlly(name)))
                .tooltipKey("blpc.party.tooltip.ally")
                .build();
    }
}
