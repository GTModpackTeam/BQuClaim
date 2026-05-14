package com.github.gtexpert.blpc.client.gui.party;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import com.github.gtexpert.blpc.client.gui.GuiColors;
import com.github.gtexpert.blpc.client.gui.PlayerFaceDrawable;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.ClientPartyCache;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyRole;

/** Shared constants, layout helpers, and live-update plumbing for party UI panels. */
public final class PartyWidgets {

    public static final int STANDARD_W = 220;
    public static final int STANDARD_H = 180;
    public static final int LARGE_W = 260;
    public static final int LARGE_H = 220;
    public static final int DIALOG_W = 220;
    public static final int DIALOG_H = 70;
    public static final int BTN_H = 18;
    public static final int FACE_SIZE = 8;
    public static final int TAB_H = 16;

    private PartyWidgets() {}

    /**
     * Per-instance unique panel name suffix. MUI's {@code PanelManager.panelHandlerMap}
     * keys on {@link ModularPanel#getName()} and never removes entries; a fresh
     * {@code IPanelHandler.simple(...)} that opens the same panel name silently
     * redirects to the stale first handler ("Using existing panel handler!" in
     * MUI log). Appending a monotonic suffix to every rebuilt panel/dialog name
     * makes each instance unique so the lookup hits the current handler.
     */
    public static String uniquePanelId(String base) {
        return base + "#" + System.nanoTime();
    }

    /** Centered title + close button at the top of the panel. */
    public static void addHeader(ModularPanel panel, IKey title) {
        panel.child(title.color(GuiColors.WHITE).shadow(true)
                .asWidget().alignment(Alignment.Center).left(0).right(0).top(8).height(10));
        panel.child(ButtonWidget.panelCloseButton());
    }

    public static void addHeader(ModularPanel panel, String titleKey) {
        addHeader(panel, IKey.lang(titleKey));
    }

    /** Positions {@code list} below the header (top=22). */
    @SuppressWarnings("rawtypes")
    public static void addList(ModularPanel panel, ListWidget list) {
        list.left(8).right(8).top(22).bottom(8);
        list.crossAxisAlignment(Alignment.CrossAxis.START);
        panel.child(list);
    }

    /**
     * Resolves a player UUID via party name cache → local player → network info
     * → truncated UUID. The cache check covers offline players.
     */
    public static String getDisplayName(UUID uuid) {
        for (Party p : ClientPartyCache.getAllParties()) {
            String cached = p.getPlayerName(uuid);
            if (cached != null) return cached;
        }
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

    public static int getRoleColor(PartyRole role) {
        return switch (role) {
            case OWNER -> GuiColors.GOLD;
            case ADMIN -> GuiColors.GREEN;
            case MEMBER -> GuiColors.WHITE;
        };
    }

    /** Button that opens {@code handler}'s dialog (rebuilding it first). */
    public static ButtonWidget<?> dialogButton(IKey label, IPanelHandler handler) {
        return (ButtonWidget<?>) new ButtonWidget<>()
                .overlay(label)
                .onMousePressed(btn -> {
                    handler.deleteCachedPanel();
                    handler.openPanel();
                    return true;
                });
    }

    /**
     * Sends {@code action} to the server, applies {@code optimistic} to the
     * cached party (if still present), then fires sync listeners so live-update
     * panels rebuild. Returns {@code true} for use as an {@code onMousePressed} body.
     */
    public static boolean sendAndApply(IMessage action, UUID partyId, Consumer<Party> optimistic) {
        ModNetwork.INSTANCE.sendToServer(action);
        Party p = ClientPartyCache.getParty(partyId);
        if (p != null) optimistic.accept(p);
        ClientPartyCache.fireSyncListeners();
        return true;
    }

    /**
     * Runs {@code onSync} on the next tick after each server sync. Auto-removed
     * on panel close. Deferred via {@link Minecraft#addScheduledTask} so a click
     * handler that triggers {@link ClientPartyCache#fireSyncListeners} doesn't
     * mutate the widget tree it's currently inside.
     */
    public static void addSyncRefreshListener(ModularPanel panel, Runnable onSync) {
        Runnable syncListener = () -> {
            if (!panel.isOpen()) return;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (panel.isOpen()) onSync.run();
            });
        };
        ClientPartyCache.addSyncListener(syncListener);
        panel.onCloseAction(() -> ClientPartyCache.removeSyncListener(syncListener));
    }

    /** Closes only when {@code panel} is on top — leaves parent panels alone. */
    public static void closeIfTopMost(ModularPanel panel) {
        if (!panel.isOpen()) return;
        if (panel.getScreen() == null) return;
        if (panel.getScreen().getPanelManager().getTopMostPanel() != panel) return;
        panel.closeIfOpen();
    }

    /**
     * Re-fetches the live {@link Party} from {@link ClientPartyCache} on every
     * call. {@link ClientPartyCache#loadFromNBT} replaces every instance on
     * sync, so reads through this supplier stay current.
     */
    public static Supplier<Party> livePartyRef(UUID partyId, Party fallback) {
        return () -> {
            Party fresh = ClientPartyCache.getParty(partyId);
            return fresh != null ? fresh : fallback;
        };
    }

    /** Gray "no rows" placeholder sized to match a {@link #BTN_H} row. */
    public static IWidget emptyStateRow(String langKey) {
        return IKey.lang(langKey).color(GuiColors.GRAY)
                .asWidget().widthRel(1f).height(BTN_H).marginLeft(4);
    }

    /** Optimistically sets the local BQu link flag for the current player. */
    public static void setLocalBQuLinked(boolean linked) {
        UUID playerId = Minecraft.getMinecraft().player.getUniqueID();
        ClientPartyCache.setLocalBQuLinked(playerId, linked);
        ClientPartyCache.fireSyncListeners();
    }

    /** Optimistically clears local party data (used after disband). */
    public static void clearLocalPartyData() {
        UUID playerId = Minecraft.getMinecraft().player.getUniqueID();
        ClientPartyCache.setLocalBQuLinked(playerId, false);
        ClientPartyCache.clear();
        ClientPartyCache.fireSyncListeners();
    }

    /** Tab row + paged content positioned below the header. */
    public static void addTabs(ModularPanel panel, PagedWidget.Controller controller,
                               String[] labelKeys, IWidget[] pages) {
        var tabRow = buildTabRow(controller, labelKeys).left(4).right(4).top(22).height(TAB_H);
        var paged = buildPagedArea(controller, pages).left(4).right(4).top(40).bottom(4);
        panel.child(tabRow);
        panel.child(paged);
    }

    /** Tab row + paged content as a single {@link Flow#column} that fills its parent. */
    public static IWidget buildInnerTabs(String[] labelKeys, IWidget[] pages) {
        var controller = new PagedWidget.Controller();
        var tabRow = buildTabRow(controller, labelKeys).widthRel(1f).height(TAB_H);
        var paged = buildPagedArea(controller, pages).widthRel(1f).expanded();
        return Flow.column().widthRel(1f).heightRel(1f).child(tabRow).child(paged);
    }

    private static Flow buildTabRow(PagedWidget.Controller controller, String[] labelKeys) {
        var row = Flow.row().childPadding(2);
        for (int i = 0; i < labelKeys.length; i++) {
            row.child(new PageButton(i, controller).height(TAB_H).expanded()
                    .overlay(IKey.lang(labelKeys[i])));
        }
        return row;
    }

    private static PagedWidget<?> buildPagedArea(PagedWidget.Controller controller, IWidget[] pages) {
        var paged = new PagedWidget<>().controller(controller);
        for (IWidget page : pages) paged.addPage(page);
        return paged;
    }

    /**
     * Search field that toggles {@code setEnabled} on the parallel widgets to
     * filter via {@link ListWidget#collapseDisabledChild} (true by default).
     */
    public static Flow wrapWithSearchBox(ListWidget<IWidget, ?> list,
                                         List<IWidget> widgets, List<String> searchNames) {
        String[] filterText = { "" };
        var searchBox = new TextFieldWidget()
                .widthRel(1f).height(14)
                .hintText(IKey.lang("blpc.party.search").get())
                .autoUpdateOnChange(true)
                .value(new StringValue.Dynamic(
                        () -> filterText[0],
                        text -> {
                            filterText[0] = text;
                            String lower = text.toLowerCase(Locale.ROOT);
                            for (int i = 0; i < widgets.size(); i++) {
                                widgets.get(i).setEnabled(lower.isEmpty() || searchNames.get(i).contains(lower));
                            }
                        }));

        return Flow.column()
                .child(searchBox)
                .child(list.widthRel(1f).expanded());
    }

    /**
     * Adds an empty-state row and returns {@code list} when {@code widgets} is
     * empty; otherwise returns {@link #wrapWithSearchBox}. Use for one-shot
     * (non-live) searchable lists built from a snapshot.
     */
    public static IWidget finalizeSearchableList(ListWidget<IWidget, ?> list, List<IWidget> widgets,
                                                 List<String> searchNames, String emptyKey) {
        if (widgets.isEmpty()) {
            list.child(emptyStateRow(emptyKey));
            return list;
        }
        return wrapWithSearchBox(list, widgets, searchNames);
    }

    /** Formats {@code "Name [Role]"} (or just {@code name} when role is null). */
    public static String formatMemberLabel(String name, PartyRole role) {
        if (role == null) return name;
        String roleStr = IKey.lang("blpc.party.role." + role.name().toLowerCase(Locale.ROOT)).get();
        return name + " [" + roleStr + "]";
    }

    /** Face-icon + label as a {@link Flow#row}; the label is supplied pre-styled. */
    public static Flow faceRow(UUID uuid, IKey label) {
        return Flow.row()
                .widthRel(1f).heightRel(1f)
                .padding(4, 0, 0, 0)
                .childPadding(4)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .child(new PlayerFaceDrawable(uuid).asWidget().size(FACE_SIZE, FACE_SIZE))
                .child(label.asWidget().expanded());
    }

    /** Clickable {@link #faceRow} with a colored, shadowed label. */
    public static ButtonWidget<?> createPlayerRow(UUID uuid, String label, int color) {
        var btn = new ButtonWidget<>();
        btn.widthRel(1f).height(BTN_H).padding(0);
        btn.hoverBackground(new Rectangle().color(GuiColors.HOVER));
        btn.child(faceRow(uuid, IKey.str(label).color(color).shadow(true).alignment(Alignment.CenterLeft)));
        return btn;
    }

    /** Text field that runs {@code onSubmit} on Enter. */
    public static TextFieldWidget createEnterSubmitTextField(Runnable onSubmit) {
        return new TextFieldWidget() {

            @Override
            public Interactable.Result onKeyPressed(char c, int keyCode) {
                if (keyCode == Keyboard.KEY_RETURN) {
                    onSubmit.run();
                    return Interactable.Result.SUCCESS;
                }
                return super.onKeyPressed(c, keyCode);
            }
        };
    }
}
