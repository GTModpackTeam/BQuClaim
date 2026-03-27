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
 * Enemy management panel (panel ID: {@value #PANEL_ID}).
 * <p>
 * FTB Utilities style: shows ALL online players except party members.
 * Enemies shown in red, non-enemies in gray. Click to toggle status.
 */
public class EnemiesPanel {

    public static final String PANEL_ID = "blpc.party.enemies";

    public static ModularPanel build(Party party) {
        Set<UUID> memberIds = new HashSet<>(party.getMembers().keySet());

        return PlayerListPanel.builder(PANEL_ID)
                .title("blpc.party.enemies_title")
                .activeSet(party.getEnemies())
                .activeColor(0xFFFF5555)
                .inactiveColor(0xFFAAAAAA)
                .excludeUUIDs(memberIds)
                .onActivate(name -> ModNetwork.INSTANCE.sendToServer(MessagePartyAction.addEnemy(name)))
                .onDeactivate(name -> ModNetwork.INSTANCE.sendToServer(MessagePartyAction.removeEnemy(name)))
                .tooltipKey("blpc.party.tooltip.enemy")
                .build();
    }
}
