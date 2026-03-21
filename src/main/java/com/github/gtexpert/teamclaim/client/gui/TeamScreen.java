package com.github.gtexpert.teamclaim.client.gui;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import com.github.gtexpert.teamclaim.api.party.PartyProviderRegistry;
import com.github.gtexpert.teamclaim.common.network.MessageTeamAction;
import com.github.gtexpert.teamclaim.common.network.ModNetwork;
import com.github.gtexpert.teamclaim.common.party.ClientPartyCache;
import com.github.gtexpert.teamclaim.common.party.Party;
import com.github.gtexpert.teamclaim.common.party.PartyRole;

public class TeamScreen {

    private static final int PANEL_W = 220;
    private static final int PANEL_H = 180;
    private static final int ROW_H = 12;

    public static ModularPanel buildAsPanel(UUID playerId, Runnable onRefresh) {
        Party myParty = ClientPartyCache.getPartyByPlayer(playerId);

        ModularPanel panel = new ModularPanel("teamclaim.party");
        panel.size(PANEL_W, PANEL_H);

        if (myParty == null) {
            buildCreateView(panel, onRefresh);
        } else {
            buildManageView(panel, myParty, playerId);
        }

        return panel;
    }

    private static void buildCreateView(ModularPanel panel, Runnable onRefresh) {
        TextFieldWidget nameField = new TextFieldWidget();
        nameField.size(140, 14).pos(8, 28);
        nameField.setText(IKey.lang(Party.DEFAULT_NAME_KEY).get());

        panel.child(IKey.lang("teamclaim.party.create_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8))
                .child(nameField)
                .child(new ButtonWidget<>().size(50, 14).pos(154, 28)
                        .overlay(IKey.lang("teamclaim.party.create"))
                        .onMousePressed(btn -> {
                            String name = nameField.getText().trim();
                            if (!name.isEmpty()) {
                                ModNetwork.INSTANCE.sendToServer(MessageTeamAction.create(name));
                                if (onRefresh != null) {
                                    ClientPartyCache.setOnSyncCallback(onRefresh);
                                }
                            }
                            panel.closeIfOpen();
                            return true;
                        }))
                .child(ButtonWidget.panelCloseButton());
    }

    private static void buildManageView(ModularPanel panel, Party party, UUID playerId) {
        panel.child(IKey.str(party.getName()).color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8));

        panel.child(ButtonWidget.panelCloseButton());

        // Member list
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
            if (y > PANEL_H - 40) break;
        }

        int btnY = PANEL_H - 22;

        boolean bquLinked = ClientPartyCache.isBQuLinked(playerId);

        if (bquLinked && PartyProviderRegistry.hasNativeScreen()) {
            // BQu linked: button to open BQu's party screen + unlink button
            panel.child(new ButtonWidget<>().size(80, 16).pos(8, btnY)
                    .overlay(IKey.lang("teamclaim.party.open_native"))
                    .onMousePressed(btn -> {
                        panel.closeIfOpen();
                        Minecraft.getMinecraft().addScheduledTask(PartyProviderRegistry::openNativeScreen);
                        return true;
                    }));
            panel.child(new ButtonWidget<>().size(80, 16).pos(PANEL_W - 88, btnY)
                    .overlay(IKey.lang("teamclaim.party.unlink_bqu"))
                    .onMousePressed(btn -> {
                        ModNetwork.INSTANCE.sendToServer(MessageTeamAction.toggleBQuLink(false));
                        panel.closeIfOpen();
                        return true;
                    }));
        } else if (!bquLinked && PartyProviderRegistry.hasNativeScreen()) {
            // BQu available but not linked: show full management + link button
            buildSelfManagedButtons(panel, party, playerId, btnY);
            panel.child(new ButtonWidget<>().size(80, 16).pos(PANEL_W - 88, btnY - 20)
                    .overlay(IKey.lang("teamclaim.party.link_bqu"))
                    .onMousePressed(btn -> {
                        ModNetwork.INSTANCE.sendToServer(MessageTeamAction.toggleBQuLink(true));
                        panel.closeIfOpen();
                        return true;
                    }));
        } else {
            buildSelfManagedButtons(panel, party, playerId, btnY);
        }
    }

    private static void buildSelfManagedButtons(ModularPanel panel, Party party, UUID playerId, int btnY) {
        PartyRole myRole = party.getRole(playerId);
        boolean isOwner = myRole == PartyRole.OWNER;

        if (myRole != null && myRole.canInvite()) {
            panel.child(new ButtonWidget<>().size(50, 16).pos(8, btnY)
                    .overlay(IKey.lang("teamclaim.party.invite"))
                    .onMousePressed(btn -> {
                        openInviteDialog(panel, party);
                        return true;
                    }));
        }

        if (myRole != null && myRole != PartyRole.OWNER) {
            panel.child(new ButtonWidget<>().size(50, 16).pos(62, btnY)
                    .overlay(IKey.lang("teamclaim.party.leave"))
                    .onMousePressed(btn -> {
                        ModNetwork.INSTANCE.sendToServer(MessageTeamAction.kickOrLeave(
                                party.getPartyId(),
                                Minecraft.getMinecraft().player.getName()));
                        panel.closeIfOpen();
                        return true;
                    }));
        }

        if (isOwner) {
            panel.child(new ButtonWidget<>().size(50, 16).pos(PANEL_W - 58, btnY)
                    .overlay(IKey.lang("teamclaim.party.disband"))
                    .onMousePressed(btn -> {
                        openDisbandConfirmDialog(panel, party);
                        return true;
                    }));
        }
    }

    private static void openDisbandConfirmDialog(ModularPanel parentPanel, Party party) {
        IPanelHandler.simple(parentPanel, (pp, player) -> {
            Dialog<Boolean> dialog = new Dialog<>("teamclaim.party.dialog.disband_confirm", result -> {
                if (Boolean.TRUE.equals(result)) {
                    ModNetwork.INSTANCE.sendToServer(MessageTeamAction.disband(party.getPartyId()));
                    parentPanel.closeIfOpen();
                }
            });
            dialog.setDisablePanelsBelow(true);
            dialog.setCloseOnOutOfBoundsClick(true);
            dialog.size(220, 70)
                    .child(IKey.lang("teamclaim.party.disband_confirm_title").color(0xFFFFFFFF).shadow(true)
                            .asWidget().top(6).left(8))
                    .child(IKey.lang("teamclaim.party.disband_confirm_msg").color(0xFFAAAAAA).shadow(true)
                            .asWidget().top(18).left(8))
                    .child(new ButtonWidget<>().size(80, 16).pos(10, 48)
                            .overlay(IKey.lang("teamclaim.party.disband_yes"))
                            .onMousePressed(btn -> {
                                dialog.closeWith(true);
                                return true;
                            }))
                    .child(new ButtonWidget<>().size(80, 16).pos(130, 48)
                            .overlay(IKey.lang("teamclaim.party.disband_no"))
                            .onMousePressed(btn -> {
                                dialog.closeWith(false);
                                return true;
                            }));
            return dialog;
        }, true).openPanel();
    }

    private static void openInviteDialog(ModularPanel parentPanel, Party party) {
        IPanelHandler.simple(parentPanel, (pp, player) -> {
            Dialog<Void> inviteDialog = new Dialog<>("teamclaim.party.dialog.invite");
            inviteDialog.setCloseOnOutOfBoundsClick(true);

            TextFieldWidget inviteField = new TextFieldWidget();
            inviteField.size(120, 14).pos(8, 24);

            inviteDialog.size(180, 60)
                    .child(IKey.lang("teamclaim.party.invite_title").color(0xFFFFFFFF).shadow(true)
                            .asWidget().pos(8, 6))
                    .child(inviteField)
                    .child(new ButtonWidget<>().size(40, 14).pos(132, 24)
                            .overlay(IKey.lang("teamclaim.party.send"))
                            .onMousePressed(btn -> {
                                String username = inviteField.getText().trim();
                                if (!username.isEmpty()) {
                                    ModNetwork.INSTANCE.sendToServer(
                                            MessageTeamAction.invite(party.getPartyId(), username));
                                }
                                inviteDialog.closeIfOpen();
                                return true;
                            }))
                    .child(ButtonWidget.panelCloseButton());
            return inviteDialog;
        }, true).openPanel();
    }

    private static String getMemberDisplayName(UUID uuid) {
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

    private static int getRoleColor(PartyRole role) {
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
