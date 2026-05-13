package com.github.gtexpert.blpc.client.gui.party;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;

import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.client.gui.party.widget.LiveSearchableList;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

/**
 * Member roles list (panel ID: {@value #PANEL_ID}). OWNER cycles others between
 * MEMBER ↔ ADMIN; OWNER transfer lives in {@link TransferOwnerDialog}.
 */
public class ModeratorsPanel {

    public static final String PANEL_ID = "blpc.party.moderators";

    public static ModularPanel build(Party party) {
        UUID playerId = Minecraft.getMinecraft().player.getUniqueID();
        UUID partyId = party.getPartyId();
        boolean[] isOwnerRef = { party.getRole(playerId) == PartyRole.OWNER };

        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
        PartyWidgets.addHeader(panel, "blpc.party.moderators_title");

        LiveSearchableList<MemberEntry> rowList = new LiveSearchableList<>(
                entry -> createRow(entry, partyId, isOwnerRef[0], playerId),
                entry -> entry.name,
                "blpc.party.no_players_online");

        // margin (not left/right/top/bottom): Flow.column already pre-fills sizeRel(1f, 1f).
        panel.child(rowList.buildContainer().margin(8, 8, 22, 8));

        rowList.rebuild(collectSorted(party));

        PartyWidgets.addSyncRefreshListener(panel, () -> {
            Party fresh = ClientPartyCache.getParty(partyId);
            if (fresh == null || !fresh.isMember(playerId)) {
                PartyWidgets.closeIfTopMost(panel);
                return;
            }
            isOwnerRef[0] = fresh.getRole(playerId) == PartyRole.OWNER;
            rowList.rebuild(collectSorted(fresh));
        });

        return panel;
    }

    private static List<MemberEntry> collectSorted(Party party) {
        List<MemberEntry> result = new ArrayList<>();
        for (Map.Entry<UUID, PartyRole> e : party.getMembers().entrySet()) {
            result.add(new MemberEntry(e.getKey(), PartyWidgets.getDisplayName(e.getKey()), e.getValue()));
        }
        // OWNER first, then ADMIN, then MEMBER; alphabetical within role.
        result.sort((a, b) -> {
            int cmp = b.role.ordinal() - a.role.ordinal();
            if (cmp != 0) return cmp;
            return a.name.compareToIgnoreCase(b.name);
        });
        return result;
    }

    private static IWidget createRow(MemberEntry entry, UUID partyId, boolean isOwner, UUID myId) {
        boolean canEdit = isOwner && !entry.uuid.equals(myId) && entry.role != PartyRole.OWNER;

        if (!canEdit) {
            int color = entry.role == PartyRole.MEMBER ? GuiColors.GRAY_LIGHT :
                    PartyWidgets.getRoleColor(entry.role);
            String label = PartyWidgets.formatMemberLabel(entry.name, entry.role);
            return PartyWidgets.createPlayerRow(entry.uuid, label, color);
        }

        PartyRole[] cycleRoles = { PartyRole.MEMBER, PartyRole.ADMIN };
        CycleButtonWidget cycle = new CycleButtonWidget()
                .length(2)
                .value(new IntValue.Dynamic(
                        () -> currentRole(partyId, entry.uuid) == PartyRole.ADMIN ? 1 : 0,
                        idx -> {
                            PartyRole r = idx == 1 ? PartyRole.ADMIN : PartyRole.MEMBER;
                            if (r == currentRole(partyId, entry.uuid)) return;
                            PartyWidgets.sendAndApply(MessagePartyAction.changeRole(entry.name + ":" + r.name()),
                                    partyId, p -> p.setRole(entry.uuid, r));
                        }))
                .stateChild(0, memberRowDisplay(entry, PartyRole.MEMBER))
                .stateChild(1, memberRowDisplay(entry, PartyRole.ADMIN))
                .widthRel(1f).height(PartyWidgets.BTN_H)
                .addTooltipLine(IKey.lang("blpc.party.tooltip.moderator"))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.options"));
        for (PartyRole r : cycleRoles) {
            cycle.addTooltipLine(IKey.dynamic(() -> formatRoleOptionLine(r, currentRole(partyId, entry.uuid))));
        }
        return cycle;
    }

    private static PartyRole currentRole(UUID partyId, UUID memberId) {
        Party current = ClientPartyCache.getParty(partyId);
        return current != null ? current.getRole(memberId) : null;
    }

    private static String formatRoleOptionLine(PartyRole option, PartyRole current) {
        String name = IKey.lang("blpc.party.role." + option.name().toLowerCase(Locale.ROOT)).get();
        // OWNER isn't a cycle option — treat as MEMBER for highlight comparison.
        PartyRole shown = current == PartyRole.OWNER ? PartyRole.MEMBER : current;
        if (option == shown) {
            return TextFormatting.YELLOW + "→ " + TextFormatting.WHITE + name;
        }
        return TextFormatting.GRAY + "  " + name;
    }

    private static IWidget memberRowDisplay(MemberEntry entry, PartyRole role) {
        return PartyWidgets.faceRow(entry.uuid, IKey.str(PartyWidgets.formatMemberLabel(entry.name, role))
                .color(PartyWidgets.getRoleColor(role)).shadow(true).alignment(Alignment.CenterLeft));
    }

    private static class MemberEntry {

        final UUID uuid;
        final String name;
        final PartyRole role;

        MemberEntry(UUID uuid, String name, PartyRole role) {
            this.uuid = uuid;
            this.name = name;
            this.role = role;
        }
    }
}
