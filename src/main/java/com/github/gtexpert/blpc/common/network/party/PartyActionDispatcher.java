package com.github.gtexpert.blpc.common.network.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.BLPCSaveHandler;
import com.github.gtexpert.blpc.common.chunk.ChunkManagerData;
import com.github.gtexpert.blpc.common.network.MessagePartyAction;
import com.github.gtexpert.blpc.common.network.MessagePartyEventNotify;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.party.DefaultPartyProvider;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;
import com.github.gtexpert.blpc.common.party.PartyRole;
import com.github.gtexpert.blpc.common.party.TrustAction;
import com.github.gtexpert.blpc.common.party.TrustLevel;

/**
 * Server-side dispatcher for {@link MessagePartyAction}. Each {@code ACTION_*} discriminator
 * maps to a single private method below.
 * <p>
 * <b>Authorization:</b> the active provider is re-derived per request from
 * {@link PartyManagerData#isBQuLinked} so a malicious client cannot bypass BQu integration.
 * Role checks happen via {@link #getAdminParty} / {@link #getOrCreateSelfParty} in each
 * mutating action.
 * <p>
 * <b>Wire protocol stability:</b> this dispatcher only consumes the integer discriminator
 * and string payload defined by {@link MessagePartyAction}; the on-wire format lives there.
 */
public final class PartyActionDispatcher implements IMessageHandler<MessagePartyAction, IMessage> {

    private static final DefaultPartyProvider SELF_PROVIDER = new DefaultPartyProvider();

    @Override
    public IMessage onMessage(MessagePartyAction msg, MessageContext ctx) {
        FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> dispatch(msg, ctx));
        return null;
    }

    private static void dispatch(MessagePartyAction msg, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        IPartyProvider provider = PartyProviderRegistry.get();
        boolean playerBQuLinked = PartyManagerData.getInstance().isBQuLinked(player.getUniqueID());
        // When not BQu-linked, use self-managed provider to avoid accidentally
        // creating/modifying BQu parties.
        IPartyProvider activeProvider = playerBQuLinked ? provider : SELF_PROVIDER;

        ActionContext c = new ActionContext(player, msg.getStringArg(), provider, SELF_PROVIDER, activeProvider,
                playerBQuLinked, new ArrayList<>());

        boolean success = switch (msg.getAction()) {
            case MessagePartyAction.ACTION_CREATE -> createParty(c);
            case MessagePartyAction.ACTION_DISBAND -> disbandParty(c);
            case MessagePartyAction.ACTION_RENAME -> renameParty(c);
            case MessagePartyAction.ACTION_INVITE -> invitePlayer(c);
            case MessagePartyAction.ACTION_ACCEPT_INVITE -> acceptInvite(c);
            case MessagePartyAction.ACTION_KICK_OR_LEAVE -> kickOrLeave(c);
            case MessagePartyAction.ACTION_CHANGE_ROLE -> changeRole(c);
            case MessagePartyAction.ACTION_TOGGLE_BQU_LINK -> toggleBQuLink(c);
            case MessagePartyAction.ACTION_TOGGLE_FAKE_PLAYERS -> toggleFakePlayers(c);
            case MessagePartyAction.ACTION_TOGGLE_EXPLOSION_PROTECTION -> toggleExplosionProtection(c);
            case MessagePartyAction.ACTION_ADD_ALLY, MessagePartyAction.ACTION_REMOVE_ALLY, MessagePartyAction.ACTION_ADD_ENEMY, MessagePartyAction.ACTION_REMOVE_ENEMY -> updateRelation(
                    c, msg.getAction());
            case MessagePartyAction.ACTION_TRANSFER_OWNERSHIP -> transferOwnership(c);
            case MessagePartyAction.ACTION_SET_TRUST_LEVEL -> setTrustLevel(c);
            case MessagePartyAction.ACTION_SET_FAKEPLAYER_TRUST -> setFakePlayerTrust(c);
            case MessagePartyAction.ACTION_SET_FREE_TO_JOIN -> setFreeToJoin(c);
            case MessagePartyAction.ACTION_SET_COLOR -> setColor(c);
            case MessagePartyAction.ACTION_SET_DESCRIPTION -> setDescription(c);
            case MessagePartyAction.ACTION_SET_MAX_MEMBERS -> setMaxMembers(c);
            case MessagePartyAction.ACTION_JOIN_FREE_PARTY -> joinFreeParty(c);
            default -> false;
        };

        // BQu link toggle always pushes a sync even when the toggle short-circuited,
        // because the provider state may have shifted underneath us.
        if (success || msg.getAction() == MessagePartyAction.ACTION_TOGGLE_BQU_LINK) {
            c.provider.syncToAll();
        }
        if (success) {
            BLPCSaveHandler.INSTANCE.markDirty();
            for (Runnable notification : c.pendingNotifications) {
                notification.run();
            }
        }
    }

    // -- Per-action handlers ------------------------------------------------------------

    private static boolean createParty(ActionContext c) {
        String name = c.stringArg.trim();
        if (name.isEmpty()) name = "New Party";
        if (name.length() > 32) name = name.substring(0, 32);
        return c.selfProvider.createParty(c.player, name);
    }

    private static boolean disbandParty(ActionContext c) {
        getOrCreateSelfParty(c.player, c.provider);
        PartyManagerData pm = PartyManagerData.getInstance();
        Party party = pm.getPartyByPlayer(c.player.getUniqueID());
        if (party == null) return false;

        PartyRole role = party.getRole(c.player.getUniqueID());
        boolean isOwnerOrOp = (role == PartyRole.OWNER) || c.player.canUseCommand(2, "");
        // BQu Link 状態では BQu 側のロールも確認する
        if (!isOwnerOrOp && c.playerBQuLinked) {
            String providerRole = c.provider.getRole(c.player.getUniqueID());
            isOwnerOrOp = PartyRole.fromName(providerRole) == PartyRole.OWNER;
        }
        if (!isOwnerOrOp) return false;

        // Authorization verified — disband directly to bypass the internal role check in
        // DefaultPartyProvider.disbandParty(), which would reject non-OWNER self-managed
        // roles even when BQu-linked as OWNER.
        List<UUID> members = new ArrayList<>(party.getMemberUUIDs());
        ChunkManagerData chunks = ChunkManagerData.getInstance();
        for (UUID memberId : members) {
            chunks.releaseAllClaims(memberId, c.player.world);
        }
        pm.removeParty(party.getPartyId());
        for (UUID memberId : members) {
            pm.setBQuLinked(memberId, false);
        }
        MinecraftServer srv = c.player.getServer();
        c.pendingNotifications.add(() -> {
            for (UUID memberId : members) {
                EntityPlayerMP member = srv != null ? srv.getPlayerList().getPlayerByUUID(memberId) : null;
                if (member != null) {
                    notifyPlayer(member, MessagePartyEventNotify.DISBANDED, "", "");
                }
            }
        });
        return true;
    }

    private static boolean renameParty(ActionContext c) {
        String newName = c.stringArg.trim();
        if (newName.length() > 32) newName = newName.substring(0, 32);
        if (newName.isEmpty()) return false;
        return c.activeProvider.renameParty(c.player, newName);
    }

    private static boolean invitePlayer(ActionContext c) {
        Party inviterParty = PartyManagerData.getInstance().getPartyByPlayer(c.player.getUniqueID());
        if (inviterParty != null && !inviterParty.canAddMember()) {
            notifyPlayer(c.player, MessagePartyEventNotify.PARTY_FULL, "", "");
            return false;
        }
        if (!c.activeProvider.invitePlayer(c.player, c.stringArg)) return false;

        MinecraftServer srv = c.player.getServer();
        if (srv != null) {
            EntityPlayerMP target = srv.getPlayerList().getPlayerByUsername(c.stringArg);
            if (target != null) {
                Party party = PartyManagerData.getInstance().getPartyByPlayer(c.player.getUniqueID());
                String partyName = party != null ? party.getName() : c.provider.getPartyName(c.player.getUniqueID());
                String resolvedPartyName = partyName != null ? partyName : "";
                String inviterName = c.player.getName();
                c.pendingNotifications.add(() -> notifyPlayer(target, MessagePartyEventNotify.INVITE_RECEIVED,
                        inviterName, resolvedPartyName));
            }
        }
        return true;
    }

    private static boolean acceptInvite(ActionContext c) {
        UUID partyId;
        try {
            partyId = UUID.fromString(c.stringArg);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        Party targetParty = PartyManagerData.getInstance().getParty(partyId);
        if (targetParty != null && !targetParty.canAddMember()) {
            notifyPlayer(c.player, MessagePartyEventNotify.PARTY_FULL, "", "");
            return false;
        }
        if (!c.activeProvider.acceptInvite(c.player, partyId)) return false;

        Party joinedParty = PartyManagerData.getInstance().getPartyByPlayer(c.player.getUniqueID());
        if (joinedParty != null) {
            String joinerName = c.player.getName();
            MinecraftServer srv = c.player.getServer();
            c.pendingNotifications.add(() -> notifyPartyMembers(joinedParty, MessagePartyEventNotify.MEMBER_JOINED,
                    joinerName, "", srv));
        }
        return true;
    }

    private static boolean kickOrLeave(ActionContext c) {
        Party party = PartyManagerData.getInstance().getPartyByPlayer(c.player.getUniqueID());
        boolean isSelf = c.stringArg.equals(c.player.getName());
        Map<UUID, PartyRole> membersCopy = party != null ? new HashMap<>(party.getMembers()) : Collections.emptyMap();

        UUID targetUUID = null;
        if (isSelf) {
            targetUUID = c.player.getUniqueID();
        } else if (party != null) {
            MinecraftServer srv = c.player.getServer();
            for (var kv : membersCopy.entrySet()) {
                EntityPlayerMP onlineMember = srv != null ? srv.getPlayerList().getPlayerByUUID(kv.getKey()) : null;
                if (onlineMember != null && onlineMember.getName().equals(c.stringArg)) {
                    targetUUID = kv.getKey();
                    break;
                }
            }
        }
        if (!c.activeProvider.kickOrLeave(c.player, c.stringArg)) return false;

        if (targetUUID != null) {
            PartyManagerData.getInstance().setBQuLinked(targetUUID, false);
        }
        if (party != null) {
            String event = isSelf ? MessagePartyEventNotify.MEMBER_LEFT : MessagePartyEventNotify.KICKED;
            UUID finalTarget = targetUUID;
            String targetName = c.stringArg;
            MinecraftServer srv = c.player.getServer();
            c.pendingNotifications.add(() -> {
                for (var entry : membersCopy.entrySet()) {
                    if (finalTarget != null && entry.getKey().equals(finalTarget)) continue;
                    EntityPlayerMP member = srv != null ? srv.getPlayerList().getPlayerByUUID(entry.getKey()) : null;
                    if (member != null) {
                        notifyPlayer(member, event, targetName, "");
                    }
                }
            });
        }
        return true;
    }

    private static boolean changeRole(ActionContext c) {
        String[] parts = c.stringArg.split(":", 2);
        if (parts.length != 2) return false;
        if (!c.activeProvider.changeRole(c.player, parts[0], parts[1])) return false;

        MinecraftServer srv = c.player.getServer();
        if (srv != null) {
            EntityPlayerMP target = srv.getPlayerList().getPlayerByUsername(parts[0]);
            if (target != null) {
                String targetName = parts[0];
                String newRole = parts[1];
                c.pendingNotifications.add(() -> notifyPlayer(target, MessagePartyEventNotify.ROLE_CHANGED,
                        targetName, newRole));
            }
        }
        return true;
    }

    private static boolean toggleBQuLink(ActionContext c) {
        boolean linked = "true".equals(c.stringArg);
        PartyManagerData pm = PartyManagerData.getInstance();
        Party currentParty = pm.getPartyByPlayer(c.player.getUniqueID());
        if (currentParty != null) {
            PartyRole role = currentParty.getRole(c.player.getUniqueID());
            if (role != null && !role.canInvite() && !c.player.canUseCommand(2, "")) {
                return false;
            }
        }
        if (linked) {
            if (!c.provider.hasNativeParty(c.player.getUniqueID())) return false;
            pm.setBQuLinked(c.player.getUniqueID(), true);
        } else {
            if (!pm.isBQuLinked(c.player.getUniqueID())) return false;
            pm.setBQuLinked(c.player.getUniqueID(), false);
            getOrCreateSelfParty(c.player, c.provider);
        }
        Party party = pm.getPartyByPlayer(c.player.getUniqueID());
        if (party != null) {
            String event = linked ? MessagePartyEventNotify.BQU_LINKED : MessagePartyEventNotify.BQU_UNLINKED;
            MinecraftServer srv = c.player.getServer();
            c.pendingNotifications.add(() -> notifyPartyMembers(party, event, "", "", srv));
        }
        return true;
    }

    /**
     * Retained for wire-protocol stability — current UI uses ACTION_SET_FAKEPLAYER_TRUST
     * to set a level directly. Cycles NONE → ALLY → MEMBER → NONE for legacy clients
     * still sending action=8.
     */
    private static boolean toggleFakePlayers(ActionContext c) {
        Party party = getOrCreateSelfParty(c.player, c.activeProvider);
        if (party == null) return false;
        PartyRole role = party.getRole(c.player.getUniqueID());
        if (role == null || !role.canInvite()) return false;
        TrustLevel next = switch (party.getFakePlayerTrustLevel()) {
            case NONE -> TrustLevel.ALLY;
            case ALLY -> TrustLevel.MEMBER;
            default -> TrustLevel.NONE;
        };
        party.setFakePlayerTrustLevel(next);
        return true;
    }

    private static boolean toggleExplosionProtection(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        party.setProtectExplosions("true".equals(c.stringArg));
        return true;
    }

    private static boolean updateRelation(ActionContext c, int action) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        UUID targetPartyId;
        try {
            targetPartyId = UUID.fromString(c.stringArg);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (targetPartyId.equals(party.getPartyId())) return false;
        switch (action) {
            case MessagePartyAction.ACTION_ADD_ALLY -> party.addAlly(targetPartyId);
            case MessagePartyAction.ACTION_REMOVE_ALLY -> party.removeAlly(targetPartyId);
            case MessagePartyAction.ACTION_ADD_ENEMY -> party.addEnemy(targetPartyId);
            case MessagePartyAction.ACTION_REMOVE_ENEMY -> party.removeEnemy(targetPartyId);
        }
        return true;
    }

    private static boolean transferOwnership(ActionContext c) {
        Party party = getOrCreateSelfParty(c.player, c.activeProvider);
        if (party == null) return false;
        PartyRole role = party.getRole(c.player.getUniqueID());
        if (role != PartyRole.OWNER && !c.player.canUseCommand(2, "")) return false;
        MinecraftServer srv = c.player.getServer();
        if (srv == null) return false;
        EntityPlayerMP target = srv.getPlayerList().getPlayerByUsername(c.stringArg);
        if (target == null) return false;
        if (!party.isMember(target.getUniqueID())) return false;

        party.setRole(target.getUniqueID(), PartyRole.OWNER);
        String newOwnerName = target.getName();
        String senderName = c.player.getName();
        EntityPlayerMP sender = c.player;
        c.pendingNotifications.add(() -> {
            notifyPlayer(target, MessagePartyEventNotify.OWNER_TRANSFERRED, newOwnerName, "");
            notifyPlayer(sender, MessagePartyEventNotify.ROLE_CHANGED, senderName, "ADMIN");
        });
        return true;
    }

    private static boolean setTrustLevel(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        String[] parts = c.stringArg.split(":", 2);
        if (parts.length != 2) return false;
        TrustAction ta = TrustAction.fromNbtKey(parts[0]);
        TrustLevel tl = TrustLevel.fromName(parts[1]);
        if (ta == null || tl.ordinal() > TrustLevel.MEMBER.ordinal()) return false;
        party.setTrustLevel(ta, tl);
        return true;
    }

    private static boolean setFakePlayerTrust(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        TrustLevel level = TrustLevel.fromName(c.stringArg);
        if (level.ordinal() > TrustLevel.MEMBER.ordinal()) return false;
        party.setFakePlayerTrustLevel(level);
        return true;
    }

    private static boolean setFreeToJoin(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        party.setFreeToJoin("true".equals(c.stringArg));
        return true;
    }

    private static boolean setColor(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        try {
            party.setColor(Integer.parseInt(c.stringArg));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean setDescription(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        String desc = c.stringArg.trim();
        if (desc.length() > 256) desc = desc.substring(0, 256);
        party.setDescription(desc);
        return true;
    }

    private static boolean setMaxMembers(ActionContext c) {
        Party party = getAdminParty(c.player, c.activeProvider);
        if (party == null) return false;
        try {
            int max = Integer.parseInt(c.stringArg);
            party.setMaxMembers(Math.min(100, Math.max(0, max)));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean joinFreeParty(ActionContext c) {
        UUID joinId;
        try {
            joinId = UUID.fromString(c.stringArg);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        PartyManagerData pm = PartyManagerData.getInstance();
        if (pm.getPartyByPlayer(c.player.getUniqueID()) != null) return false;
        Party party = pm.getParty(joinId);
        if (party == null || !party.isFreeToJoin()) return false;
        if (!party.canAddMember()) {
            notifyPlayer(c.player, MessagePartyEventNotify.PARTY_FULL, "", "");
            return false;
        }
        party.addMember(c.player.getUniqueID(), PartyRole.MEMBER);
        String joinerName = c.player.getName();
        MinecraftServer srv = c.player.getServer();
        c.pendingNotifications.add(() -> notifyPartyMembers(party, MessagePartyEventNotify.MEMBER_JOINED,
                joinerName, "", srv));
        return true;
    }

    // -- Shared helpers -----------------------------------------------------------------

    private static Party getOrCreateSelfParty(EntityPlayerMP player, IPartyProvider provider) {
        PartyManagerData pmData = PartyManagerData.getInstance();
        Party party = pmData.getPartyByPlayer(player.getUniqueID());
        if (party == null) {
            String partyName = provider.getPartyName(player.getUniqueID());
            if (partyName != null) {
                party = pmData.createParty(partyName, player.getUniqueID());
            }
        }
        return party;
    }

    private static Party getAdminParty(EntityPlayerMP player, IPartyProvider provider) {
        Party party = getOrCreateSelfParty(player, provider);
        if (party == null) return null;
        PartyRole role = party.getRole(player.getUniqueID());
        if (role == null || !role.canInvite()) return null;
        return party;
    }

    private static void notifyPartyMembers(Party party, String eventType, String playerName, String extra,
                                           MinecraftServer server) {
        if (server == null) return;
        MessagePartyEventNotify packet = new MessagePartyEventNotify(eventType, playerName, extra);
        for (UUID memberId : party.getMembers().keySet()) {
            EntityPlayerMP member = server.getPlayerList().getPlayerByUUID(memberId);
            if (member != null) ModNetwork.INSTANCE.sendTo(packet, member);
        }
    }

    private static void notifyPlayer(EntityPlayerMP player, String eventType, String playerName, String extra) {
        ModNetwork.INSTANCE.sendTo(new MessagePartyEventNotify(eventType, playerName, extra), player);
    }

    /** Per-request state bundle, scoped to a single {@link #dispatch}. */
    private static final class ActionContext {

        final EntityPlayerMP player;
        final String stringArg;
        final IPartyProvider provider;
        final DefaultPartyProvider selfProvider;
        final IPartyProvider activeProvider;
        final boolean playerBQuLinked;
        final List<Runnable> pendingNotifications;

        ActionContext(EntityPlayerMP player, String stringArg, IPartyProvider provider,
                      DefaultPartyProvider selfProvider, IPartyProvider activeProvider, boolean playerBQuLinked,
                      List<Runnable> pendingNotifications) {
            this.player = player;
            this.stringArg = stringArg;
            this.provider = provider;
            this.selfProvider = selfProvider;
            this.activeProvider = activeProvider;
            this.playerBQuLinked = playerBQuLinked;
            this.pendingNotifications = pendingNotifications;
        }
    }
}
