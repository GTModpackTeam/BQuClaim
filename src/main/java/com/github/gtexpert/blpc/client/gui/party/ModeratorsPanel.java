package com.github.gtexpert.blpc.client.gui.party;

import java.util.*;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

/**
 * Moderator management panel (panel ID: {@value #PANEL_ID}).
 * <p>
 * FTB Utilities style: shows ALL party members in a single list.
 * ADMINs shown in green, MEMBERs in gray, OWNER in gold.
 * OWNER can click to promote MEMBER -> ADMIN or demote ADMIN -> MEMBER.
 * Cannot change OWNER or self.
 */
public class ModeratorsPanel {

    public static final String PANEL_ID = "blpc.party.moderators";

    public static ModularPanel build(Party party) {
        UUID playerId = Minecraft.getMinecraft().player.getUniqueID();
        PartyRole myRole = party.getRole(playerId);
        boolean isOwner = myRole == PartyRole.OWNER;

        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);

        PartyWidgets.addHeader(panel, "blpc.party.moderators_title");

        var sorted = new ArrayList<>(party.getMembers().entrySet());
        sorted.sort((a, b) -> {
            // OWNER first, then ADMIN, then MEMBER
            int cmp = b.getValue().ordinal() - a.getValue().ordinal();
            if (cmp != 0) return cmp;
            return PartyWidgets.getDisplayName(a.getKey())
                    .compareToIgnoreCase(PartyWidgets.getDisplayName(b.getKey()));
        });

        ListWidget<?, ?> list = new ListWidget<>()
                .children(sorted, entry -> createRow(entry, party, isOwner, playerId));

        PartyWidgets.addList(panel, list);

        PartyWidgets.addSyncCloseListener(panel);

        return panel;
    }

    private static ButtonWidget<?> createRow(Map.Entry<UUID, PartyRole> entry, Party party,
                                             boolean isOwner, UUID myId) {
        UUID memberId = entry.getKey();
        PartyRole role = entry.getValue();
        String memberName = PartyWidgets.getDisplayName(memberId);
        int color = role == PartyRole.MEMBER ? GuiColors.GRAY_LIGHT : PartyWidgets.getRoleColor(role);
        String label = PartyWidgets.formatMemberLabel(memberName, role);

        ButtonWidget<?> btn = PartyWidgets.createPlayerRow(memberId, label, color);

        if (isOwner && !memberId.equals(myId) && role != PartyRole.OWNER) {
            String newRole = switch (role) {
                case MEMBER -> "ADMIN";
                case ADMIN -> "MEMBER";
                case OWNER -> throw new AssertionError();
            };
            PartyRole newPartyRole = PartyRole.fromName(newRole);
            btn.onMousePressed(b -> {
                ModNetwork.INSTANCE.sendToServer(
                        MessagePartyAction.changeRole(memberName + ":" + newRole));
                if (newPartyRole != null) {
                    party.setRole(memberId, newPartyRole);
                    ClientPartyCache.fireSyncListeners();
                }
                return true;
            });
            btn.addTooltipLine(IKey.lang("blpc.party.tooltip.moderator"));
        }

        return btn;
    }
}
