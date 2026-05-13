package com.github.gtexpert.blpc.client.gui.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

/**
 * Fluent builder for the party main menu. Accumulate entries via
 * {@link #navHandler} / {@link #nav} / {@link #widget}, then materialize with
 * {@link #buildInto}.
 */
public final class PartyMenuBuilder {

    private final MenuContext context;
    private final List<EntryDef> entries = new ArrayList<>();
    private EntryDef current;

    private PartyMenuBuilder(MenuContext context) {
        this.context = context;
    }

    public static PartyMenuBuilder of(ModularPanel panel, Party party, UUID playerId) {
        boolean bquAvailable = PartyProviderRegistry.hasNativeScreen();
        return new PartyMenuBuilder(new MenuContext(party, playerId, panel, bquAvailable));
    }

    /** Appends a nav entry that builds a fresh sub-panel via {@code factory} on click. */
    public PartyMenuBuilder nav(String labelKey, Function<Party, ModularPanel> factory) {
        finalizeCurrent();
        current = new EntryDef(labelKey, factory);
        return this;
    }

    /**
     * Appends a nav entry that opens a pre-created {@link IPanelHandler}. Prefer
     * this when the menu is rebuilt across syncs — MUI's {@code clientSubPanels}
     * has no removal API, so reusing the handler keeps the list bounded.
     */
    public PartyMenuBuilder navHandler(String labelKey, IPanelHandler handler) {
        finalizeCurrent();
        current = new EntryDef(labelKey, handler);
        return this;
    }

    public PartyMenuBuilder widget(IWidget widget) {
        finalizeCurrent();
        current = new EntryDef(widget);
        return this;
    }

    public PartyMenuBuilder tooltip(String langKey) {
        requireCurrent();
        current.tooltipKey = langKey;
        return this;
    }

    /** Skips the most recent entry when {@code condition} returns {@code false}. */
    public PartyMenuBuilder visible(Predicate<MenuContext> condition) {
        requireCurrent();
        current.visible = condition;
        return this;
    }

    @SuppressWarnings("rawtypes")
    public void buildInto(ListWidget list) {
        finalizeCurrent();
        for (EntryDef entry : entries) {
            if (entry.visible != null && !entry.visible.test(context)) continue;
            list.child(entry.createWidget(context));
        }
    }

    private void finalizeCurrent() {
        if (current != null) {
            entries.add(current);
            current = null;
        }
    }

    private void requireCurrent() {
        if (current == null) {
            throw new IllegalStateException("No current entry — call nav() or widget() first");
        }
    }

    public static final class MenuContext {

        private final Party party;
        private final UUID playerId;
        private final ModularPanel panel;
        private final PartyRole role;
        private final boolean bquAvailable;

        MenuContext(Party party, UUID playerId, ModularPanel panel, boolean bquAvailable) {
            this.party = party;
            this.playerId = playerId;
            this.panel = panel;
            this.role = party.getRole(playerId);
            this.bquAvailable = bquAvailable;
        }

        Party party() {
            return party;
        }

        ModularPanel panel() {
            return panel;
        }

        public boolean bquAvailable() {
            return bquAvailable;
        }

        public boolean canInvite() {
            return role != null && role.canInvite();
        }

        public boolean isOwner() {
            return role == PartyRole.OWNER;
        }
    }

    private static final class EntryDef {

        final String labelKey;
        final Function<Party, ModularPanel> factory;
        final IPanelHandler preCreatedHandler;
        final IWidget rawWidget;
        String tooltipKey;
        Predicate<MenuContext> visible;

        EntryDef(String labelKey, Function<Party, ModularPanel> factory) {
            this.labelKey = labelKey;
            this.factory = factory;
            this.preCreatedHandler = null;
            this.rawWidget = null;
        }

        EntryDef(String labelKey, IPanelHandler preCreatedHandler) {
            this.labelKey = labelKey;
            this.factory = null;
            this.preCreatedHandler = preCreatedHandler;
            this.rawWidget = null;
        }

        EntryDef(IWidget rawWidget) {
            this.labelKey = null;
            this.factory = null;
            this.preCreatedHandler = null;
            this.rawWidget = rawWidget;
        }

        IWidget createWidget(MenuContext ctx) {
            if (rawWidget != null) return rawWidget;

            IPanelHandler handler = preCreatedHandler != null ? preCreatedHandler :
                    IPanelHandler.simple(ctx.panel(), (pp, player) -> factory.apply(ctx.party()), true);
            ButtonWidget<?> btn = PartyWidgets.dialogButton(IKey.lang(labelKey).alignment(Alignment.CenterLeft),
                    handler)
                    .widthRel(1f).height(PartyWidgets.BTN_H)
                    .padding(4, 0, 0, 0)
                    .hoverBackground(new Rectangle().color(GuiColors.HOVER));
            if (tooltipKey != null) btn.addTooltipLine(IKey.lang(tooltipKey));
            return btn;
        }
    }
}
