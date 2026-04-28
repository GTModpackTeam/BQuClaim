package com.github.gtexpert.blpc.client.gui.party;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

/**
 * Transfer ownership panel (panel ID: {@value #PANEL_ID}).
 * <p>
 * Displays a scrollable list of party members (excluding the current owner).
 * Clicking a member's name transfers ownership to them.
 */

public class TransferOwnerDialog {

    public static final String PANEL_ID = "blpc.party.dialog.transfer";

    public static ModularPanel build(Party party) {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        if (party == null) return new ModularPanel(PANEL_ID);

        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);

        PartyWidgets.addHeader(panel, "blpc.party.transfer_title");

        ListWidget<?, ?> list = new ListWidget<>()
                .crossAxisAlignment(Alignment.CrossAxis.START)
                .children(party.getMembers().entrySet().stream()
                        .filter(e -> !e.getKey().equals(myId))
                        .collect(Collectors.toList()),
                        entry -> createTransferRow(entry, panel));

        PartyWidgets.addList(panel, list);

        PartyWidgets.addSyncCloseListener(panel);

        return panel;
    }

    private static ButtonWidget<?> createTransferRow(Map.Entry<UUID, PartyRole> entry,
                                                     ModularPanel panel) {
        UUID memberId = entry.getKey();
        String memberName = PartyWidgets.getDisplayName(memberId);
        PartyRole role = entry.getValue();
        String label = PartyWidgets.formatMemberLabel(memberName, role);

        ButtonWidget<?> btn = PartyWidgets.createPlayerRow(memberId, label,
                PartyWidgets.getRoleColor(role));
        btn.onMousePressed(b -> {
            ModNetwork.INSTANCE.sendToServer(
                    MessagePartyAction.transferOwnership(memberName));
            UUID myId = Minecraft.getMinecraft().player.getUniqueID();
            Party p = ClientPartyCache.getPartyByPlayer(myId);
            if (p != null) {
                p.setRole(memberId, PartyRole.OWNER);
            }
            ClientPartyCache.fireSyncListeners();
            return true;
        });
        return btn;
    }
}
