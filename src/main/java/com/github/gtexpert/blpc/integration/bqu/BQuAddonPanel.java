package com.github.gtexpert.blpc.integration.bqu;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.client.gui.party.PartyWidgets;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.network.message.PartyAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/**
 * BetterQuesting addon settings (panel ID: {@value #PANEL_ID}). Reached from
 * the party menu's Addons hub. Hosts the BQu link/unlink toggle and the
 * native BQu party manager shortcut.
 */
@SideOnly(Side.CLIENT)
public final class BQuAddonPanel {

    public static final String PANEL_ID = "blpc.party.addons.bqu";

    private BQuAddonPanel() {}

    public static ModularPanel build(UUID playerId) {
        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        panel.size(PartyWidgets.STANDARD_W, PartyWidgets.STANDARD_H);
        PartyWidgets.addHeader(panel, "blpc.addons.bqu.title");

        @SuppressWarnings("rawtypes")
        ListWidget list = new ListWidget();

        list.child(PartyWidgets.toggleButton(
                new BoolValue.Dynamic(
                        () -> ClientPartyCache.isBQuLinked(playerId),
                        val -> {
                            PartyWidgets.setLocalBQuLinked(val);
                            ModNetwork.INSTANCE.sendToServer(PartyAction.toggleBQuLink(val));
                        }),
                "blpc.party.link_bqu", "blpc.party.unlink_bqu")
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

        list.child(new ButtonWidget<>().widthRel(1f).height(PartyWidgets.BTN_H)
                .padding(PartyWidgets.ROW_INDENT, 0, 0, 0)
                .overlay(PartyWidgets.buttonLabelLeft(IKey.lang("blpc.party.open_native")))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.open_native"))
                .setEnabledIf(w -> ClientPartyCache.isBQuLinked(playerId))
                .onMousePressed(btn -> {
                    panel.closeIfOpen();
                    Minecraft.getMinecraft().addScheduledTask(PartyProviderRegistry::openNativeScreen);
                    return true;
                }));

        PartyWidgets.addList(panel, list);
        return panel;
    }
}
