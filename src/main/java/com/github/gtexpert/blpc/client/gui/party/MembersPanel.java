package com.github.gtexpert.blpc.client.gui.party;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyRole;
import com.github.gtexpert.blpc.client.gui.BLPCColors;
import com.github.gtexpert.blpc.client.gui.party.PartyWidgets.MemberEntry;
import com.github.gtexpert.blpc.client.gui.party.widget.LiveSearchableList;
import com.github.gtexpert.blpc.common.network.message.PartyAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/**
 * Members list (panel ID: {@value #PANEL_ID}). MEMBER sees a single list;
 * ADMIN+ sees Members + Invite tabs. Closes when the player loses the party
 * or crosses the manage permission boundary (layouts differ).
 */
public class MembersPanel {

    public static final String PANEL_ID = "blpc.party.members";

    public static ModularPanel build(Party party) {
        UUID playerId = Minecraft.getMinecraft().player.getUniqueID();
        PartyRole myRole = party.getRole(playerId);
        boolean canManage = myRole != null && myRole.canInvite();
        UUID partyId = party.getPartyId();

        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        PartyRole[] myRoleRef = { myRole };

        LiveSearchableList<MemberEntry> membersList = new LiveSearchableList<>(
                entry -> createMemberRow(entry, partyId, playerId, myRoleRef[0], canManage),
                entry -> entry.name,
                "blpc.party.no_players_online");
        LiveSearchableList<MemberEntry> inviteList = canManage ? new LiveSearchableList<>(
                entry -> createInviteRow(entry, partyId),
                entry -> entry.name,
                "blpc.party.no_players_online") : null;

        if (canManage) {
            panel.size(PartyWidgets.LARGE_W, PartyWidgets.LARGE_H);
            PartyWidgets.addHeader(panel, "blpc.party.members_title");
            PartyWidgets.addTabs(panel, new PagedWidget.Controller(),
                    new String[] { "blpc.party.tab.members", "blpc.party.tab.invite" },
                    new IWidget[] { membersList.buildContainer(), inviteList.buildContainer() });
        } else {
            panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
            PartyWidgets.addHeader(panel, "blpc.party.members_title");
            // margin (not left/right/top/bottom): Flow.column already pre-fills sizeRel(1f, 1f).
            panel.child(PartyWidgets.fillBelowHeader(membersList.buildContainer()));
        }

        membersList.rebuild(collectMembers(party));
        if (canManage) inviteList.rebuild(collectInvitableOnlinePlayers(party));

        PartyWidgets.addSyncRefreshListener(panel, () -> {
            Party fresh = ClientPartyCache.getParty(partyId);
            if (fresh == null || !fresh.isMember(playerId)) {
                PartyWidgets.closeIfTopMost(panel);
                return;
            }
            PartyRole freshRole = fresh.getRole(playerId);
            boolean freshCanManage = freshRole != null && freshRole.canInvite();
            if (freshCanManage != canManage) {
                PartyWidgets.closeIfTopMost(panel);
                return;
            }
            myRoleRef[0] = freshRole;
            membersList.rebuild(collectMembers(fresh));
            if (freshCanManage) inviteList.rebuild(collectInvitableOnlinePlayers(fresh));
        });

        return panel;
    }

    private static List<MemberEntry> collectMembers(Party party) {
        return PartyWidgets.collectSortedMembers(party, null);
    }

    private static List<MemberEntry> collectInvitableOnlinePlayers(Party party) {
        List<MemberEntry> result = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getConnection() == null) return result;
        for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
            UUID uuid = info.getGameProfile().getId();
            if (party.isMember(uuid) || party.hasInvite(uuid)) continue;
            result.add(new MemberEntry(uuid, info.getGameProfile().getName(), null));
        }
        result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    private static IWidget createMemberRow(MemberEntry entry, UUID partyId, UUID playerId,
                                           PartyRole myRole, boolean canManage) {
        int color = PartyWidgets.getRoleColor(entry.role);
        String label = PartyWidgets.formatMemberLabel(entry.name, entry.role);
        ButtonWidget<?> btn = PartyWidgets.createPlayerRow(entry.uuid, label, color);

        boolean isSelf = entry.uuid.equals(playerId);
        // Self can leave (non-owner only); admins+ can kick lower-ranked members.
        boolean canSelfLeave = isSelf && entry.role != PartyRole.OWNER;
        boolean canKickOther = !isSelf && canManage && entry.role != null && myRole != null &&
                myRole.canKick(entry.role);
        if (canSelfLeave || canKickOther) {
            String playerName = entry.name;
            btn.onMousePressed(b -> PartyWidgets.sendAndApply(
                    PartyAction.kickOrLeave(playerName), partyId, p -> p.removeMember(entry.uuid)));
            btn.addTooltipLine(IKey.lang(canSelfLeave ? "blpc.party.tooltip.member_self" : "blpc.party.tooltip.kick"));
        }

        return btn;
    }

    private static IWidget createInviteRow(MemberEntry entry, UUID partyId) {
        ButtonWidget<?> btn = PartyWidgets.createPlayerRow(entry.uuid, entry.name, BLPCColors.inactive());
        String playerName = entry.name;
        btn.onMousePressed(b -> PartyWidgets.sendAndApply(
                PartyAction.invite(playerName), partyId, p -> p.addInvite(entry.uuid, Long.MAX_VALUE)));
        btn.addTooltipLine(IKey.lang("blpc.party.tooltip.invite"));
        return btn;
    }
}
