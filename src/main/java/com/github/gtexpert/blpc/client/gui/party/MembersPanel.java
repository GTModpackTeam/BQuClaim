package com.github.gtexpert.blpc.client.gui.party;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

public class MembersPanel {

    public static final String PANEL_ID = "blpc.party.members";
    private static final int W = 220;
    private static final int H = 180;
    private static final int ROW_H = 12;

    public static ModularPanel build(Party party) {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        panel.child(IKey.lang("blpc.party.members_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8));
        panel.child(ButtonWidget.panelCloseButton());

        int y = 26;
        Map<UUID, PartyRole> members = party.getMembers();
        for (Map.Entry<UUID, PartyRole> entry : members.entrySet()) {
            UUID memberId = entry.getKey();
            PartyRole role = entry.getValue();
            String memberName = getMemberDisplayName(memberId);
            String roleStr = role.name().substring(0, 1) + role.name().substring(1).toLowerCase();

            panel.child(IKey.str(memberName + " [" + roleStr + "]")
                    .color(getRoleColor(role)).shadow(true)
                    .asWidget().pos(8, y));

            y += ROW_H;
            if (y > H - 20) break;
        }

        return panel;
    }

    static String getMemberDisplayName(UUID uuid) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.player.getUniqueID().equals(uuid)) {
            return mc.player.getName();
        }
        if (mc.getConnection() != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
            if (info != null) return info.getGameProfile().getName();
        }
        return uuid.toString().substring(0, 8);
    }

    static int getRoleColor(PartyRole role) {
        switch (role) {
            case OWNER:
                return 0xFFFFAA00;
            case ADMIN:
                return 0xFF55FF55;
            default:
                return 0xFFFFFFFF;
        }
    }
}
