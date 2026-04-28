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
 * Fluent builder for composing the party main menu.
 * <p>
 * Inspired by GregTech's two-phase UI builder pattern: accumulate entries
 * via {@link #nav} / {@link #widget}, then materialize them into a
 * {@link ListWidget} with {@link #buildInto}.
 * <p>
 * Navigation entries use {@code Function<Party, ModularPanel>} so panel
 * factories can be passed as method references (e.g. {@code MembersPanel::build}).
 * The builder supplies the {@link Party} instance automatically.
 *
 * <pre>
 * {@code
 * PartyMenuBuilder.of(panel, party, playerId)
 *     .nav("blpc.party.members", MembersPanel::build)
 *     .nav("blpc.party.settings", SettingsPanel::build)
 *         .tooltip("blpc.party.tooltip.settings")
 *         .visible(MenuContext::canInvite)
 *     .widget(myToggle)
 *         .visible(ctx -> ctx.bquAvailable())
 *     .buildInto(menuList);
 * }
 * </pre>
 */
public final class PartyMenuBuilder {

    private final MenuContext context;
    private final List<EntryDef> entries = new ArrayList<>();
    private EntryDef current;

    private PartyMenuBuilder(MenuContext context) {
        this.context = context;
    }

    /**
     * Creates a new builder with context derived from the current game state.
     *
     * @param panel    the parent panel (used as IPanelHandler parent)
     * @param party    the player's current party
     * @param playerId the local player's UUID
     */
    public static PartyMenuBuilder of(ModularPanel panel, Party party, UUID playerId) {
        boolean bquAvailable = PartyProviderRegistry.hasNativeScreen();
        return new PartyMenuBuilder(new MenuContext(party, playerId, panel, bquAvailable));
    }

    /**
     * Appends a navigation entry that opens a sub-panel.
     * <p>
     * The factory receives the current {@link Party} — use a method reference:
     * {@code .nav("key", MembersPanel::build)}
     */
    public PartyMenuBuilder nav(String labelKey, Function<Party, ModularPanel> factory) {
        finalizeCurrent();
        current = new EntryDef(labelKey, factory);
        return this;
    }

    /**
     * Appends a raw widget (toggle buttons, special actions, etc.).
     */
    public PartyMenuBuilder widget(IWidget widget) {
        finalizeCurrent();
        current = new EntryDef(widget);
        return this;
    }

    /** Sets a tooltip on the most recently added entry. */
    public PartyMenuBuilder tooltip(String langKey) {
        requireCurrent();
        current.tooltipKey = langKey;
        return this;
    }

    /**
     * Sets a visibility predicate on the most recently added entry.
     * The entry is skipped entirely when the predicate returns {@code false}.
     * <p>
     * Use {@link MenuContext} method references for common cases:
     * <ul>
     * <li>{@code .visible(MenuContext::canInvite)} — ADMIN or OWNER</li>
     * <li>{@code .visible(MenuContext::isOwner)} — OWNER only</li>
     * <li>{@code .visible(ctx -> ctx.bquAvailable())} — custom logic</li>
     * </ul>
     */
    public PartyMenuBuilder visible(Predicate<MenuContext> condition) {
        requireCurrent();
        current.visible = condition;
        return this;
    }

    /**
     * Materializes all accumulated entries into the given {@link ListWidget}.
     * Entries whose visibility predicate returns {@code false} are skipped.
     * Each navigation entry gets its own dedicated {@link IPanelHandler}.
     */
    @SuppressWarnings("rawtypes")
    public void buildInto(ListWidget list) {
        finalizeCurrent();
        for (EntryDef entry : entries) {
            if (entry.visible != null && !entry.visible.test(context)) continue;
            list.child(entry.createWidget(context));
        }
    }

    /** Returns the context for external use (e.g. building widgets outside the builder). */
    public MenuContext context() {
        return context;
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

    /**
     * Immutable context passed to menu entry predicates and factories.
     * Holds the current party state, player identity, and UI references.
     */
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

        public Party party() {
            return party;
        }

        public UUID playerId() {
            return playerId;
        }

        public ModularPanel panel() {
            return panel;
        }

        public PartyRole role() {
            return role;
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
        final IWidget rawWidget;
        String tooltipKey;
        Predicate<MenuContext> visible;

        EntryDef(String labelKey, Function<Party, ModularPanel> factory) {
            this.labelKey = labelKey;
            this.factory = factory;
            this.rawWidget = null;
        }

        EntryDef(IWidget rawWidget) {
            this.labelKey = null;
            this.factory = null;
            this.rawWidget = rawWidget;
        }

        @SuppressWarnings("unchecked")
        IWidget createWidget(MenuContext ctx) {
            if (rawWidget != null) return rawWidget;

            IPanelHandler handler = IPanelHandler.simple(
                    ctx.panel(), (pp, player) -> factory.apply(ctx.party()), true);

            ButtonWidget<?> btn = (ButtonWidget<?>) new ButtonWidget<>()
                    .widthRel(1f).height(PartyWidgets.BTN_H)
                    .padding(4, 0, 0, 0)
                    .hoverBackground(new Rectangle().color(GuiColors.HOVER))
                    .overlay(IKey.lang(labelKey).alignment(Alignment.CenterLeft))
                    .onMousePressed(b -> {
                        handler.deleteCachedPanel();
                        handler.openPanel();
                        return true;
                    });
            if (tooltipKey != null) {
                btn.addTooltipLine(IKey.lang(tooltipKey));
            }
            return btn;
        }
    }
}
