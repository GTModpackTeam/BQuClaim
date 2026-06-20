package com.github.gtexpert.blpc.client.gui.party;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.TrustAction;
import com.github.gtexpert.blpc.api.party.TrustLevel;
import com.github.gtexpert.blpc.client.gui.BLPCColors;
import com.github.gtexpert.blpc.client.gui.party.widget.InputDialog;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.network.message.PartyAction;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;

/**
 * Settings panel (panel ID: {@value #PANEL_ID}). Four tabs: Party Info,
 * Protection, Allies, Enemies. All widgets read through {@link PartyWidgets#livePartyRef}
 * so they track the cache across syncs without rebuilding.
 */
public class SettingsPanel {

    public static final String PANEL_ID = "blpc.party.settings";
    private static final int BTN_H = PartyWidgets.BTN_H;

    private static final TrustLevel[] CYCLE_LEVELS = { TrustLevel.NONE, TrustLevel.ALLY, TrustLevel.MEMBER };

    public static ModularPanel build(Party initialParty) {
        ModularPanel panel = new ModularPanel(PartyWidgets.uniquePanelId(PANEL_ID));
        panel.size(PartyWidgets.LARGE_W, PartyWidgets.LARGE_H);

        UUID partyId = initialParty.getPartyId();
        Supplier<Party> partyRef = PartyWidgets.livePartyRef(partyId, initialParty);

        PartyWidgets.addHeader(panel, "blpc.party.settings_title");

        var controller = new PagedWidget.Controller();
        PartyWidgets.addTabs(panel, controller,
                new String[] {
                        "blpc.party.settings_tab_party",
                        "blpc.party.settings_tab_protection",
                        "blpc.party.settings_tab_allies",
                        "blpc.party.settings_tab_enemies"
                },
                new IWidget[] {
                        buildPartyInfoPage(partyRef, panel),
                        buildProtectionPage(partyRef),
                        buildAlliesPage(partyRef),
                        buildEnemiesPage(partyRef)
                });

        PartyWidgets.addSyncRefreshListener(panel, () -> {
            if (ClientPartyCache.getParty(partyId) == null) {
                PartyWidgets.closeIfTopMost(panel);
            }
        });

        return panel;
    }

    private static IWidget buildPartyInfoPage(Supplier<Party> partyRef, ModularPanel panel) {
        var list = newList();

        // Pre-create handlers to avoid "same panel handler already exists" on repeated clicks.
        IPanelHandler renameHandler = IPanelHandler.simple(panel, (pp, player) -> InputDialog
                .builder(PartyWidgets.uniquePanelId("blpc.party.dialog.rename"))
                .title("blpc.party.name_field")
                .defaultValue(partyRef.get().getName())
                .confirmLabel("blpc.map.yes")
                .onSubmit(text -> {
                    partyRef.get().setName(text);
                    ModNetwork.INSTANCE.sendToServer(PartyAction.rename(text));
                })
                .build(), true);

        IPanelHandler descHandler = IPanelHandler.simple(panel, (pp, player) -> InputDialog
                .builder(PartyWidgets.uniquePanelId("blpc.party.dialog.description"))
                .title("blpc.party.description_field")
                .defaultValue(partyRef.get().getDescription())
                .confirmLabel("blpc.map.yes")
                .onSubmit(text -> {
                    partyRef.get().setDescription(text);
                    ModNetwork.INSTANCE.sendToServer(PartyAction.setDescription(text));
                })
                .build(), true);

        IPanelHandler colorHandler = IPanelHandler.simple(panel, (pp, player) -> {
            int startArgb = BLPCColors.partyArgb(partyRef.get().getColor());
            var dialog = new ColorPickerDialog(color -> {
                int rgb = color & 0xFFFFFF;
                partyRef.get().setColor(rgb);
                ModNetwork.INSTANCE.sendToServer(PartyAction.setColor(rgb));
            }, startArgb, false);
            dialog.setCloseOnOutOfBoundsClick(true);
            return dialog;
        }, true);

        list.child(PartyWidgets.dialogButton(
                IKey.dynamic(() -> IKey.lang("blpc.party.name_field").get() + ": " + partyRef.get().getName())
                        .alignment(Alignment.CenterLeft),
                renameHandler)
                .addTooltipLine(PartyWidgets.underlineKey("blpc.party.tooltip.name"))
                .addTooltipLine(IKey.dynamic(() -> PartyWidgets.defaultTooltip("\"\"")))
                .widthRel(1f).height(BTN_H).padding(PartyWidgets.ROW_INDENT, 0, 0, 0));

        list.child(PartyWidgets.dialogButton(
                IKey.dynamic(() -> IKey.lang("blpc.party.description_field").get() + ": " +
                        (partyRef.get().getDescription().isEmpty() ? "-" : partyRef.get().getDescription()))
                        .alignment(Alignment.CenterLeft),
                descHandler)
                .addTooltipLine(PartyWidgets.underlineKey("blpc.party.tooltip.description"))
                .addTooltipLine(IKey.dynamic(() -> PartyWidgets.defaultTooltip("\"\"")))
                .widthRel(1f).height(BTN_H).padding(PartyWidgets.ROW_INDENT, 0, 0, 0));

        list.child(PartyWidgets.divider());

        list.child(PartyWidgets.dialogButton(
                IKey.dynamic(() -> IKey.lang("blpc.party.color").get() + ": " +
                        formatColorHex(partyRef.get().getColor()))
                        .alignment(Alignment.CenterLeft),
                colorHandler)
                .addTooltipLine(PartyWidgets.underlineKey("blpc.party.tooltip.color"))
                .widthRel(1f).height(BTN_H).padding(PartyWidgets.ROW_INDENT, 0, 0, 0));

        list.child(PartyWidgets.toggleButton(
                new BoolValue.Dynamic(
                        () -> partyRef.get().isFreeToJoin(),
                        val -> {
                            partyRef.get().setFreeToJoin(val);
                            ModNetwork.INSTANCE.sendToServer(PartyAction.setFreeToJoin(val));
                        }),
                "blpc.party.free_to_join_off", "blpc.party.free_to_join_on")
                .addTooltipLine(PartyWidgets.underlineKey("blpc.party.tooltip.free_to_join"))
                .addTooltipLine(IKey.dynamic(() -> PartyWidgets.defaultTooltip("false"))));

        list.child(IKey.dynamic(() -> buildMaxMembersLabel(partyRef.get()))
                .alignment(Alignment.CenterLeft)
                .asWidget().widthRel(1f).height(10).marginLeft(4).marginTop(4)
                .addTooltipLine(PartyWidgets.underlineKey("blpc.party.tooltip.max_members"))
                .addTooltipLine(IKey.dynamic(() -> PartyWidgets.defaultTooltip("0"))));

        list.child(new SliderWidget()
                .widthRel(1f).height(10).marginLeft(4).marginRight(4).marginBottom(4)
                .bounds(0, 100).stopper(1)
                .value(new DoubleValue.Dynamic(
                        () -> partyRef.get().getMaxMembers(),
                        val -> {
                            int max = (int) Math.round(val);
                            Party party = partyRef.get();
                            if (max == party.getMaxMembers()) return;
                            party.setMaxMembers(max);
                            ModNetwork.INSTANCE.sendToServer(PartyAction.setMaxMembers(max));
                        })));

        return list;
    }

    private static String buildMaxMembersLabel(Party party) {
        int max = party.getMaxMembers();
        int current = party.getMembers().size();
        String value = max == 0 ? IKey.lang("blpc.party.max_members_unlimited").get() :
                current + " / " + max;
        return IKey.lang("blpc.party.max_members").get() + ": " + value;
    }

    private static IWidget buildProtectionPage(Supplier<Party> partyRef) {
        var list = newList();

        for (TrustAction action : TrustAction.values()) {
            list.child(createTrustCycle(partyRef, action));
        }
        list.child(createFakePlayerCycle(partyRef));

        list.child(PartyWidgets.divider());

        list.child(PartyWidgets.toggleButton(
                new BoolValue.Dynamic(
                        () -> partyRef.get().protectsExplosions(),
                        val -> {
                            partyRef.get().setProtectExplosions(val);
                            ModNetwork.INSTANCE.sendToServer(PartyAction.setExplosionProtection(val));
                        }),
                "blpc.party.explosion_off", "blpc.party.explosion_on")
                .addTooltipLine(PartyWidgets.underlineKey("blpc.party.tooltip.explosion"))
                .addTooltipLine(IKey.dynamic(() -> PartyWidgets.defaultTooltip("true"))));

        return list;
    }

    private static IWidget buildAlliesPage(Supplier<Party> partyRef) {
        return buildTrustPage(partyRef, false);
    }

    private static IWidget buildEnemiesPage(Supplier<Party> partyRef) {
        return buildTrustPage(partyRef, true);
    }

    /**
     * Builds the inner two-tab (Parties / Players) layout for ally or enemy management.
     * Toggle buttons update color in-place via {@link IKey#dynamicKey} — panel stays open.
     */
    private static IWidget buildTrustPage(Supplier<Party> partyRef, boolean isEnemy) {
        return PartyWidgets.buildInnerTabs(
                new String[] { "blpc.party.tab.parties", "blpc.party.tab.players" },
                new IWidget[] { buildTrustPartyList(partyRef, isEnemy),
                        buildTrustPlayerList(partyRef, isEnemy) });
    }

    private static IWidget buildTrustPartyList(Supplier<Party> partyRef, boolean isEnemy) {
        var list = newList();
        final UUID myPartyId = partyRef.get().getPartyId();
        Collection<Party> allParties = ClientPartyCache.getAllParties();
        var widgets = new ArrayList<IWidget>();
        var searchNames = new ArrayList<String>();

        for (Party other : allParties) {
            if (other.getPartyId().equals(myPartyId)) continue;
            final UUID pid = other.getPartyId();
            final String name = other.getName();

            var btn = new ButtonWidget<>()
                    .widthRel(1f).height(BTN_H).padding(PartyWidgets.ROW_INDENT, 0, 0, 0)
                    .overlay(IKey
                            .dynamicKey(() -> PartyWidgets.rowLabel(IKey.str(name), trustColor(partyRef.get(), pid))))
                    .addTooltipLine(trustTooltip(isEnemy))
                    .onMousePressed(b -> {
                        toggleTrust(partyRef.get(), pid, isEnemy);
                        return true;
                    });

            widgets.add(btn);
            searchNames.add(name.toLowerCase(Locale.ROOT));
            list.child(btn);
        }
        return PartyWidgets.finalizeSearchableList(list, widgets, searchNames, "blpc.party.no_other_parties");
    }

    private static IWidget buildTrustPlayerList(Supplier<Party> partyRef, boolean isEnemy) {
        var list = newList();
        final UUID myPartyId = partyRef.get().getPartyId();
        var conn = Minecraft.getMinecraft().getConnection();
        if (conn == null) return list;

        var widgets = new ArrayList<IWidget>();
        var searchNames = new ArrayList<String>();

        for (NetworkPlayerInfo info : conn.getPlayerInfoMap()) {
            UUID playerUUID = info.getGameProfile().getId();
            String playerName = info.getGameProfile().getName();
            Party playerParty = ClientPartyCache.getPartyByPlayer(playerUUID);

            if (playerParty != null && playerParty.getPartyId().equals(myPartyId)) continue;

            if (playerParty == null) {
                String noPartyLabel = playerName + " (" + IKey.lang("blpc.party.tab.no_party").get() + ")";
                var row = PartyWidgets.faceRow(playerUUID,
                        IKey.str(noPartyLabel).color(BLPCColors.subtext()).alignment(Alignment.CenterLeft))
                        .height(BTN_H);
                widgets.add(row);
                searchNames.add(playerName.toLowerCase(Locale.ROOT));
                list.child(row);
            } else {
                final UUID pid = playerParty.getPartyId();
                final String partyLabel = playerName + " (" + playerParty.getName() + ")";
                var btn = new ButtonWidget<>()
                        .widthRel(1f).height(BTN_H).padding(0)
                        .child(PartyWidgets.faceRow(playerUUID, IKey.dynamicKey(
                                () -> PartyWidgets.rowLabel(IKey.str(partyLabel), trustColor(partyRef.get(), pid)))))
                        .addTooltipLine(trustTooltip(isEnemy))
                        .onMousePressed(b -> {
                            toggleTrust(partyRef.get(), pid, isEnemy);
                            return true;
                        });
                widgets.add(btn);
                searchNames.add(playerName.toLowerCase(Locale.ROOT));
                list.child(btn);
            }
        }
        return PartyWidgets.finalizeSearchableList(list, widgets, searchNames, "blpc.party.no_players_online");
    }

    private static void toggleTrust(Party party, UUID pid, boolean isEnemy) {
        boolean active = isEnemy ? party.isEnemy(pid) : party.isAlly(pid);
        if (active) {
            if (isEnemy) {
                party.removeEnemy(pid);
                ModNetwork.INSTANCE.sendToServer(PartyAction.removeEnemy(pid));
            } else {
                party.removeAlly(pid);
                ModNetwork.INSTANCE.sendToServer(PartyAction.removeAlly(pid));
            }
        } else {
            if (isEnemy) {
                party.addEnemy(pid);
                ModNetwork.INSTANCE.sendToServer(PartyAction.addEnemy(pid));
            } else {
                party.addAlly(pid);
                ModNetwork.INSTANCE.sendToServer(PartyAction.addAlly(pid));
            }
        }
    }

    private static int trustColor(Party party, UUID pid) {
        if (party.isAlly(pid)) return BLPCColors.owner();
        if (party.isEnemy(pid)) return BLPCColors.warning();
        // Neutral rows render on a gray button — white reads, black would not.
        return BLPCColors.buttonText();
    }

    private static IKey trustTooltip(boolean isEnemy) {
        return IKey.lang(isEnemy ? "blpc.party.tooltip.toggle_enemy" : "blpc.party.tooltip.toggle_ally");
    }

    @SuppressWarnings("unchecked")
    private static ListWidget<IWidget, ?> newList() {
        var list = new ListWidget<>();
        list.widthRel(1f).heightRel(1f);
        list.crossAxisAlignment(Alignment.CrossAxis.START);
        return list;
    }

    private static IWidget createTrustCycle(Supplier<Party> partyRef, TrustAction action) {
        return createTrustCycleCommon(
                () -> partyRef.get().getTrustLevel(action),
                level -> {
                    Party party = partyRef.get();
                    if (level == party.getTrustLevel(action)) return;
                    party.setTrustLevel(action, level);
                    ModNetwork.INSTANCE.sendToServer(
                            PartyAction.setTrustLevel(action.getNbtKey() + ":" + level.name()));
                },
                () -> buildTrustLabel(partyRef.get(), action),
                "blpc.party.tooltip.trust_level",
                () -> IKey.lang("blpc.party.trust_level." + action.getDefaultLevel().name().toLowerCase(Locale.ROOT))
                        .get());
    }

    private static IWidget createFakePlayerCycle(Supplier<Party> partyRef) {
        return createTrustCycleCommon(
                () -> partyRef.get().getFakePlayerTrustLevel(),
                level -> {
                    Party party = partyRef.get();
                    if (level == party.getFakePlayerTrustLevel()) return;
                    party.setFakePlayerTrustLevel(level);
                    ModNetwork.INSTANCE.sendToServer(PartyAction.setFakePlayerTrust(level.name()));
                },
                () -> buildFakePlayerLabel(partyRef.get()),
                "blpc.party.tooltip.fakeplayer",
                () -> IKey.lang("blpc.party.trust_level." + TrustLevel.ALLY.name().toLowerCase(Locale.ROOT)).get());
    }

    private static IWidget createTrustCycleCommon(
                                                  Supplier<TrustLevel> getter,
                                                  Consumer<TrustLevel> setter,
                                                  Supplier<String> labelBuilder,
                                                  String tooltipKey,
                                                  Supplier<String> defaultValueBuilder) {
        CycleButtonWidget cycle = new CycleButtonWidget()
                .length(CYCLE_LEVELS.length)
                .value(new IntValue.Dynamic(
                        () -> {
                            TrustLevel cur = getter.get();
                            for (int i = 0; i < CYCLE_LEVELS.length; i++) {
                                if (CYCLE_LEVELS[i] == cur) return i;
                            }
                            return 0;
                        },
                        idx -> setter.accept(CYCLE_LEVELS[idx])))
                .child(PartyWidgets.buttonLabelLeft(IKey.dynamic(labelBuilder::get))
                        .asWidget()
                        .widthRel(1f).heightRel(1f).padding(PartyWidgets.ROW_INDENT, 0, 0, 0))
                .widthRel(1f).height(BTN_H).marginBottom(2)
                .addTooltipLine(PartyWidgets.underlineKey(tooltipKey))
                .addTooltipLine(IKey.dynamic(() -> PartyWidgets.defaultTooltip(defaultValueBuilder.get())))
                .addTooltipLine(IKey.lang("blpc.party.tooltip.options"));
        // List all options; current selection is highlighted with an arrow.
        for (TrustLevel level : CYCLE_LEVELS) {
            cycle.addTooltipLine(IKey.dynamic(() -> formatTrustOptionLine(level, getter.get())));
        }
        return cycle;
    }

    private static String formatTrustOptionLine(TrustLevel option, TrustLevel current) {
        return PartyWidgets.formatCycleOptionLine("blpc.party.trust_level.", option.name(), option == current);
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

    private static String formatColorHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
