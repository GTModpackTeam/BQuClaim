package com.github.gtexpert.blpc.client.gui.party;

import java.util.Locale;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;

import com.github.gtexpert.blpc.client.gui.party.widget.InputDialog;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.TrustAction;
import com.github.gtexpert.blpc.common.party.TrustLevel;

/**
 * Protection and party settings panel (panel ID: {@value #PANEL_ID}).
 * <p>
 * Uses a scrollable {@link ListWidget} with section headers to organize
 * settings into two groups:
 * <ul>
 * <li><b>Protection</b> — trust levels per action, FakePlayer trust, explosion protection</li>
 * <li><b>Party</b> — free to join, color, title, description</li>
 * </ul>
 */
public class SettingsPanel {

    public static final String PANEL_ID = "blpc.party.settings";
    private static final int W = 260;
    private static final int H = 220;
    private static final int BTN_H = 18;

    private static final TrustLevel[] CYCLE_LEVELS = { TrustLevel.NONE, TrustLevel.ALLY, TrustLevel.MEMBER };

    private static final int[] PRESET_COLORS = {
            0x0000FF, 0xFF0000, 0x00FF00, 0xFFFF00,
            0xFF00FF, 0x00FFFF, 0xFF8800, 0xFFFFFF,
    };

    public static ModularPanel build(Party party) {
        ModularPanel panel = new ModularPanel(PANEL_ID);
        panel.size(W, H);

        panel.child(IKey.lang("blpc.party.settings_title").color(0xFFFFFFFF).shadow(true)
                .asWidget().pos(8, 8));
        panel.child(ButtonWidget.panelCloseButton());

        // Scrollable settings list with section headers
        ListWidget<com.cleanroommc.modularui.api.widget.IWidget, ?> list = new ListWidget<>();
        list.left(4).right(4).top(24).bottom(4);
        list.crossAxisAlignment(Alignment.CrossAxis.START);

        // --- Protection section ---
        list.child(sectionHeader("blpc.party.settings_protection"));

        for (TrustAction action : TrustAction.values()) {
            list.child(createTrustCycleButton(party, action));
        }
        list.child(createFakePlayerCycleButton(party));

        list.child(new ToggleButton()
                .size(W - 16, BTN_H).padding(4, 0, 0, 0)
                .value(new BoolValue.Dynamic(
                        party::protectsExplosions,
                        val -> {
                            party.setProtectExplosions(val);
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.toggleExplosionProtection());
                        }))
                .overlay(false, IKey.lang("blpc.party.explosion_off").alignment(Alignment.CenterLeft))
                .overlay(true, IKey.lang("blpc.party.explosion_on").alignment(Alignment.CenterLeft))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.explosion")));

        // --- Party section (extra top margin for visual separation) ---
        list.child(sectionHeader("blpc.party.settings_party").marginTop(8));

        list.child(new ToggleButton()
                .size(W - 16, BTN_H).padding(4, 0, 0, 0)
                .value(new BoolValue.Dynamic(
                        party::isFreeToJoin,
                        val -> {
                            party.setFreeToJoin(val);
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.setFreeToJoin(val));
                        }))
                .overlay(false, IKey.lang("blpc.party.free_to_join_off").alignment(Alignment.CenterLeft))
                .overlay(true, IKey.lang("blpc.party.free_to_join_on").alignment(Alignment.CenterLeft))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.free_to_join")));

        list.child(new CycleButtonWidget()
                .size(W - 16, BTN_H).padding(4, 0, 0, 0)
                .stateCount(PRESET_COLORS.length)
                .value(new IntValue.Dynamic(
                        () -> colorToIndex(party.getColor()),
                        val -> {
                            int color = PRESET_COLORS[val];
                            party.setColor(color);
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.setColor(color));
                        }))
                .overlay(IKey.dynamic(() -> buildColorLabel(party)).alignment(Alignment.CenterLeft))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.color")));

        list.child(PartyWidgets.createActionButton(
                IKey.dynamic(() -> IKey.lang("blpc.party.title_field").get() + ": " +
                        (party.getTitle().isEmpty() ? "-" : party.getTitle()))
                        .alignment(Alignment.CenterLeft),
                "Edit title",
                () -> IPanelHandler.simple(panel, (pp, player) -> InputDialog.builder("blpc.party.dialog.title")
                        .title("blpc.party.title_field")
                        .confirmLabel("blpc.map.yes")
                        .onSubmit(text -> {
                            party.setTitle(text);
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.setTitle(text));
                        })
                        .build(), true).openPanel())
                .addTooltipLine(IKey.lang("blpc.party.tooltip.title"))
                .size(W - 16, BTN_H).padding(4, 0, 0, 0));

        list.child(PartyWidgets.createActionButton(
                IKey.dynamic(() -> IKey.lang("blpc.party.description_field").get() + ": " +
                        (party.getDescription().isEmpty() ? "-" : party.getDescription()))
                        .alignment(Alignment.CenterLeft),
                "Edit description",
                () -> IPanelHandler.simple(panel, (pp, player) -> InputDialog
                        .builder("blpc.party.dialog.description")
                        .title("blpc.party.description_field")
                        .confirmLabel("blpc.map.yes")
                        .onSubmit(text -> {
                            party.setDescription(text);
                            ModNetwork.INSTANCE.sendToServer(MessagePartyAction.setDescription(text));
                        })
                        .build(), true).openPanel())
                .addTooltipLine(IKey.lang("blpc.party.tooltip.description"))
                .size(W - 16, BTN_H).padding(4, 0, 0, 0));

        panel.child(list);
        return panel;
    }

    private static TextWidget<?> sectionHeader(String langKey) {
        return IKey.lang(langKey).color(0xFFFFAA00).shadow(true)
                .asWidget().height(14).widthRel(1f);
    }

    private static CycleButtonWidget createTrustCycleButton(Party party, TrustAction action) {
        return new CycleButtonWidget()
                .size(W - 16, BTN_H).padding(4, 0, 0, 0)
                .stateCount(CYCLE_LEVELS.length)
                .value(new IntValue.Dynamic(
                        () -> trustLevelToIndex(party.getTrustLevel(action)),
                        val -> {
                            TrustLevel level = CYCLE_LEVELS[val];
                            party.setTrustLevel(action, level);
                            ModNetwork.INSTANCE.sendToServer(
                                    MessagePartyAction.setTrustLevel(action.getNbtKey() + ":" + level.name()));
                        }))
                .overlay(IKey.dynamic(() -> buildTrustLabel(party, action)).alignment(Alignment.CenterLeft))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.trust_level"));
    }

    private static CycleButtonWidget createFakePlayerCycleButton(Party party) {
        return new CycleButtonWidget()
                .size(W - 16, BTN_H).padding(4, 0, 0, 0)
                .stateCount(CYCLE_LEVELS.length)
                .value(new IntValue.Dynamic(
                        () -> trustLevelToIndex(party.getFakePlayerTrustLevel()),
                        val -> {
                            TrustLevel level = CYCLE_LEVELS[val];
                            party.setFakePlayerTrustLevel(level);
                            ModNetwork.INSTANCE.sendToServer(
                                    MessagePartyAction.setFakePlayerTrust(level.name()));
                        }))
                .overlay(IKey.dynamic(() -> buildFakePlayerLabel(party)).alignment(Alignment.CenterLeft))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.fakeplayer"));
    }

    private static int trustLevelToIndex(TrustLevel level) {
        for (int i = 0; i < CYCLE_LEVELS.length; i++) {
            if (CYCLE_LEVELS[i] == level) return i;
        }
        return 0;
    }

    private static String buildTrustLabel(Party party, TrustAction action) {
        TrustLevel current = party.getTrustLevel(action);
        return IKey.lang("blpc.party.trust." + action.getNbtKey()).get() + ": " +
                IKey.lang("blpc.party.trust_level." + current.name().toLowerCase(Locale.ROOT)).get();
    }

    private static String buildFakePlayerLabel(Party party) {
        TrustLevel level = party.getFakePlayerTrustLevel();
        return IKey.lang("blpc.party.fakeplayer_trust").get() + ": " +
                IKey.lang("blpc.party.trust_level." + level.name().toLowerCase(Locale.ROOT)).get();
    }

    private static int colorToIndex(int color) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == color) return i;
        }
        return 0;
    }

    private static String buildColorLabel(Party party) {
        String colorName;
        switch (party.getColor()) {
            case 0x0000FF:
                colorName = "Blue";
                break;
            case 0xFF0000:
                colorName = "Red";
                break;
            case 0x00FF00:
                colorName = "Green";
                break;
            case 0xFFFF00:
                colorName = "Yellow";
                break;
            case 0xFF00FF:
                colorName = "Magenta";
                break;
            case 0x00FFFF:
                colorName = "Cyan";
                break;
            case 0xFF8800:
                colorName = "Orange";
                break;
            case 0xFFFFFF:
                colorName = "White";
                break;
            default:
                colorName = String.format("#%06X", party.getColor());
                break;
        }
        return IKey.lang("blpc.party.color").get() + ": " + colorName;
    }
}
