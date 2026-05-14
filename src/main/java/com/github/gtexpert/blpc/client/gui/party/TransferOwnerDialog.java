package com.github.gtexpert.blpc.client.gui.party;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

/** Transfer-owner dialog (panel ID: {@value #PANEL_ID}). */
public class TransferOwnerDialog {

    public static final String PANEL_ID = "blpc.party.dialog.transfer";

    public static ModularPanel build(Party party) {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        if (party == null) return new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));

        UUID partyId = party.getPartyId();

        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
        PartyWidgets.addHeader(panel, "blpc.party.transfer_title");

        @SuppressWarnings("unchecked")
        ListWidget<IWidget, ?> list = (ListWidget<IWidget, ?>) new ListWidget<>()
                .crossAxisAlignment(Alignment.CrossAxis.START);
        PartyWidgets.addList(panel, list);

        rebuild(list, party, myId, partyId);

        PartyWidgets.addSyncRefreshListener(panel, () -> {
            Party fresh = ClientPartyCache.getParty(partyId);
            if (fresh == null || fresh.getRole(myId) != PartyRole.OWNER) {
                PartyWidgets.closeIfTopMost(panel);
                return;
            }
            rebuild(list, fresh, myId, partyId);
        });

        return panel;
    }

    private static void rebuild(ListWidget<IWidget, ?> list, Party party, UUID myId, UUID partyId) {
        list.removeAll();
        party.getMembers().entrySet().stream()
                .filter(e -> !e.getKey().equals(myId))
                .forEach(e -> list.child(createTransferRow(e, partyId)));
    }

    private static ButtonWidget<?> createTransferRow(Map.Entry<UUID, PartyRole> entry, UUID partyId) {
        UUID memberId = entry.getKey();
        String memberName = PartyWidgets.getDisplayName(memberId);
        PartyRole role = entry.getValue();

        ButtonWidget<?> btn = PartyWidgets.createPlayerRow(memberId,
                PartyWidgets.formatMemberLabel(memberName, role), PartyWidgets.getRoleColor(role));
        btn.onMousePressed(b -> PartyWidgets.sendAndApply(
                MessagePartyAction.transferOwnership(memberName), partyId,
                p -> p.setRole(memberId, PartyRole.OWNER)));
        return btn;
    }
}
