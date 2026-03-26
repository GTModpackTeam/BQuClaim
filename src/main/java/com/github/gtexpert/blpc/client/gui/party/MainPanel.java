package com.github.gtexpert.blpc.client.gui.party;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

public class MainPanel {

    public static final String PANEL_ID = "blpc.party";
    private static final int W = 220;
    private static final int H = 180;

    public static ModularPanel build(UUID playerId) {
        Party party = ClientPartyCache.getPartyByPlayer(playerId);
        boolean bquAvailable = PartyProviderRegistry.hasNativeScreen();
        boolean bquLinked = ClientPartyCache.isBQuLinked(playerId);

        // Auto-fix: bquLinked but no party → clear stale flag
        if (bquLinked && party == null) {
            ClientPartyCache.setLocalBQuLinked(playerId, false);
            bquLinked = false;
        }

        if (party == null) {
            return CreatePanel.build();
        }

        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        panel.child(IKey.str(party.getName()).color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8));
        panel.child(ButtonWidget.panelCloseButton());

        int y = 30;

        PartyRole myRole = party.getRole(playerId);
        boolean canManage = myRole != null && myRole.canInvite();
        boolean isOwner = myRole == PartyRole.OWNER;

        // Settings
        if (canManage) {
            panel.child(createMenuButton(IKey.lang("blpc.party.settings"), y, panel,
                    () -> SettingsPanel.build(party)));
            y += 22;
        }

        // Members & Invite (not needed when BQu linked — managed via BQu screen)
        if (!bquLinked) {
            panel.child(createMenuButton(IKey.lang("blpc.party.members"), y, panel,
                    () -> MembersPanel.build(party)));
            y += 22;

            if (canManage) {
                panel.child(createMenuButton(IKey.lang("blpc.party.invite"), y, panel,
                        () -> InviteDialog.build()));
                y += 22;
            }
        }

        // BQu Manage Party
        if (bquAvailable && bquLinked) {
            panel.child(new ButtonWidget<>().size(W - 16, 18).pos(8, y)
                    .overlay(IKey.lang("blpc.party.open_native"))
                    .onMousePressed(btn -> {
                        panel.closeIfOpen();
                        Minecraft.getMinecraft().addScheduledTask(PartyProviderRegistry::openNativeScreen);
                        return true;
                    }));
            y += 22;
        }

        // Bottom buttons
        int btnY = H - 24;

        if (bquAvailable && bquLinked && canManage) {
            panel.child(new ButtonWidget<>().size(80, 16).pos(8, btnY)
                    .overlay(IKey.lang("blpc.party.unlink_bqu"))
                    .onMousePressed(btn -> {
                        openSubPanel(panel, UnlinkBQuDialog.build(panel));
                        return true;
                    }));
        } else if (bquAvailable && !bquLinked && canManage) {
            panel.child(new ButtonWidget<>().size(80, 16).pos(8, btnY)
                    .overlay(IKey.lang("blpc.party.link_bqu"))
                    .onMousePressed(btn -> {
                        openSubPanel(panel, LinkBQuDialog.build(panel));
                        return true;
                    }));
        }

        if (myRole != null && myRole != PartyRole.OWNER) {
            panel.child(new ButtonWidget<>().size(50, 16).pos(W / 2 - 25, btnY)
                    .overlay(IKey.lang("blpc.party.leave"))
                    .onMousePressed(btn -> {
                        ModNetwork.INSTANCE.sendToServer(MessagePartyAction.kickOrLeave(
                                Minecraft.getMinecraft().player.getName()));
                        panel.closeIfOpen();
                        return true;
                    }));
        }

        if (isOwner) {
            panel.child(new ButtonWidget<>().size(50, 16).pos(W - 58, btnY)
                    .overlay(IKey.lang("blpc.party.disband"))
                    .onMousePressed(btn -> {
                        openSubPanel(panel, DisbandDialog.build(panel));
                        return true;
                    }));
        }

        return panel;
    }

    private static ButtonWidget<?> createMenuButton(IKey label, int y, ModularPanel parent,
                                                    PanelFactory factory) {
        return (ButtonWidget<?>) new ButtonWidget<>().size(W - 16, 18).pos(8, y)
                .overlay(label)
                .onMousePressed(btn -> {
                    openSubPanel(parent, factory.create());
                    return true;
                });
    }

    private static void openSubPanel(ModularPanel parent, ModularPanel child) {
        IPanelHandler.simple(parent, (pp, player) -> child, true).openPanel();
    }

    @FunctionalInterface
    interface PanelFactory {

        ModularPanel create();
    }
}
