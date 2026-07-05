package com.github.gtexpert.blpc.client.gui.party;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyRole;
import com.github.gtexpert.blpc.client.gui.party.PartyWidgets.MemberEntry;
import com.github.gtexpert.blpc.client.gui.party.widget.LiveSearchableList;
import com.github.gtexpert.blpc.common.network.message.PartyAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/** Transfer-ownership panel (panel ID: {@value #PANEL_ID}). OWNER-only member picker. */
public class TransferOwnerPanel {

    public static final String PANEL_ID = "blpc.party.dialog.transfer";

    public static ModularPanel build(Party party) {
        UUID myId = Minecraft.getMinecraft().player.getUniqueID();
        if (party == null) return new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));

        UUID partyId = party.getPartyId();

        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
        PartyWidgets.addHeader(panel, "blpc.party.transfer_title");

        LiveSearchableList<MemberEntry> rowList = new LiveSearchableList<>(
                entry -> createTransferRow(entry, partyId),
                MemberEntry::name,
                "blpc.party.no_players_online");
        panel.child(PartyWidgets.fillBelowHeader(rowList.buildContainer()));

        rowList.rebuild(collectTransferable(party, myId));

        PartyWidgets.addSyncRefreshListener(panel, () -> {
            Party fresh = ClientPartyCache.getParty(partyId);
            if (fresh == null || fresh.getRole(myId) != PartyRole.OWNER) {
                PartyWidgets.closeIfTopMost(panel);
                return;
            }
            rowList.rebuild(collectTransferable(fresh, myId));
        });

        return panel;
    }

    private static List<MemberEntry> collectTransferable(Party party, UUID myId) {
        return PartyWidgets.collectSortedMembers(party, myId);
    }

    private static IWidget createTransferRow(MemberEntry entry, UUID partyId) {
        ButtonWidget<?> btn = PartyWidgets.createPlayerRow(entry.uuid(),
                PartyWidgets.formatMemberLabel(entry.name(), entry.role()), PartyWidgets.getRoleColor(entry.role()));
        btn.onMousePressed(b -> PartyWidgets.sendAndApply(
                PartyAction.transferOwnership(entry.name()), partyId,
                p -> p.setRole(entry.uuid(), PartyRole.OWNER)));
        return btn;
    }
}
