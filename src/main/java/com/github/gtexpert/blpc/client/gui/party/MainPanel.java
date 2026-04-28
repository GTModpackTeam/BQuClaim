package com.github.gtexpert.blpc.client.gui.party;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;

import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.client.gui.party.widget.ConfirmDialog;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;

/**
 * Main party menu panel (panel ID: {@value #PANEL_ID}).
 * <p>
 * Entry point for all party management. Conditionally shows buttons
 * based on the player's role and BQu link status:
 * <ul>
 * <li>Settings (includes allies/enemies management) - ADMIN+ only</li>
 * <li>Members, Moderators - always shown</li>
 * <li>Transfer Ownership - OWNER only</li>
 * <li>Open BQu Party Screen - shown when BQu-linked</li>
 * <li>Link/Unlink BQu toggle - ADMIN+ only, shown when BQu available</li>
 * <li>Disband - OWNER only, bottom-pinned</li>
 * </ul>
 * If the player has no party, delegates to {@link CreatePanel}.
 */
public class MainPanel {

    public static final String PANEL_ID = "blpc.party";

    public static ModularPanel build(UUID playerId) {
        Party party = ClientPartyCache.getPartyByPlayer(playerId);
        boolean bquLinked = ClientPartyCache.isBQuLinked(playerId);

        if (bquLinked && party == null) {
            ClientPartyCache.setLocalBQuLinked(playerId, false);
        }

        if (party == null) {
            return CreatePanel.build();
        }

        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);

        panel.child(new ScrollingTextWidget(IKey.dynamic(party::getName))
                .color(GuiColors.WHITE).shadow(true)
                .alignment(Alignment.Center).left(0).right(20).top(8).height(10));
        panel.child(ButtonWidget.panelCloseButton());

        @SuppressWarnings("rawtypes")
        ListWidget menuList = new ListWidget();
        menuList.left(8).right(8).top(26).bottom(26);
        menuList.crossAxisAlignment(Alignment.CrossAxis.START);

        var builder = PartyMenuBuilder.of(panel, party, playerId);

        builder.nav("blpc.party.settings", SettingsPanel::build)
                .tooltip("blpc.party.tooltip.settings")
                .visible(PartyMenuBuilder.MenuContext::canInvite)
                .nav("blpc.party.members", MembersPanel::build)
                .tooltip("blpc.party.tooltip.members")
                .nav("blpc.party.moderators", ModeratorsPanel::build)
                .tooltip("blpc.party.tooltip.moderators")
                .nav("blpc.party.transfer", TransferOwnerDialog::build)
                .tooltip("blpc.party.tooltip.transfer")
                .visible(PartyMenuBuilder.MenuContext::isOwner);

        var ctx = builder.context();

        if (ctx.bquAvailable()) {
            builder.widget((ButtonWidget<?>) new ButtonWidget<>().widthRel(1f).height(PartyWidgets.BTN_H)
                    .padding(4, 0, 0, 0)
                    .overlay(IKey.lang("blpc.party.open_native").alignment(Alignment.CenterLeft))
                    .addTooltipLine(IKey.lang("blpc.party.tooltip.open_native"))
                    .setEnabledIf(w -> ClientPartyCache.isBQuLinked(playerId))
                    .onMousePressed(btn -> {
                        panel.closeIfOpen();
                        Minecraft.getMinecraft().addScheduledTask(PartyProviderRegistry::openNativeScreen);
                        return true;
                    }));
        }

        if (ctx.bquAvailable() && ctx.canInvite()) {
            builder.widget(new ToggleButton()
                    .widthRel(1f).height(PartyWidgets.BTN_H).padding(4, 0, 0, 0)
                    .value(new BoolValue.Dynamic(
                            () -> ClientPartyCache.isBQuLinked(playerId),
                            val -> {
                                PartyWidgets.setLocalBQuLinked(val);
                                ModNetwork.INSTANCE.sendToServer(MessagePartyAction.toggleBQuLink(val));
                            }))
                    .overlay(false, IKey.lang("blpc.party.link_bqu").alignment(Alignment.CenterLeft))
                    .overlay(true, IKey.lang("blpc.party.unlink_bqu").alignment(Alignment.CenterLeft))
                    .addTooltipLine(IKey.lang("blpc.party.tooltip.link_bqu"))
                    .addTooltipLine(IKey.dynamicKey(() -> {
                        Party myParty = ClientPartyCache.getPartyByPlayer(playerId);
                        if (myParty == null) {
                            return IKey.lang("blpc.party.tooltip.bqu_no_party").color(GuiColors.RED);
                        }
                        String ownerName = myParty.getOwner() != null ?
                                PartyWidgets.getDisplayName(myParty.getOwner()) : "?";
                        return IKey.str(IKey.lang("blpc.party.tooltip.bqu_party_info").get() + ": " +
                                myParty.getName() + " (" + ownerName + ")").color(GuiColors.GRAY);
                    })));
        }

        builder.buildInto(menuList);
        panel.child(menuList);

        if (ctx.isOwner()) {
            IPanelHandler disbandHandler = IPanelHandler.simple(
                    panel, (pp, player) -> ConfirmDialog.builder("blpc.party.dialog.disband")
                            .title("blpc.party.disband_confirm_title")
                            .message("blpc.party.disband_confirm_msg")
                            .yesLabel("blpc.party.disband_yes")
                            .noLabel("blpc.party.disband_no")
                            .closeParent(false)
                            .onConfirm(() -> {
                                ModNetwork.INSTANCE.sendToServer(MessagePartyAction.disband());
                                panel.closeIfOpen();
                                PartyWidgets.clearLocalPartyData();
                            })
                            .build(panel),
                    true);
            panel.child(PartyWidgets.createActionButton(
                    IKey.lang("blpc.party.disband"), "Open Disband dialog",
                    () -> {
                        disbandHandler.deleteCachedPanel();
                        disbandHandler.openPanel();
                    })
                    .size(50, 16).pos(PartyWidgets.STANDARD_W - 58, PartyWidgets.STANDARD_H - 24));
        }

        PartyWidgets.addSyncCloseListener(panel);

        return panel;
    }
}
