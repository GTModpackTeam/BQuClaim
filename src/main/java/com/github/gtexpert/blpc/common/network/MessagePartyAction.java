package com.github.gtexpert.blpc.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.party.DefaultPartyProvider;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;
import com.github.gtexpert.blpc.common.party.PartyRole;
import com.github.gtexpert.blpc.common.party.TrustAction;
import com.github.gtexpert.blpc.common.party.TrustLevel;

import io.netty.buffer.ByteBuf;

public class MessagePartyAction implements IMessage {

    public static final int ACTION_CREATE = 0;
    public static final int ACTION_DISBAND = 1;
    public static final int ACTION_RENAME = 2;
    public static final int ACTION_INVITE = 3;
    public static final int ACTION_ACCEPT_INVITE = 4;
    public static final int ACTION_KICK_OR_LEAVE = 5;
    public static final int ACTION_CHANGE_ROLE = 6;
    public static final int ACTION_TOGGLE_BQU_LINK = 7;
    public static final int ACTION_TOGGLE_FAKE_PLAYERS = 8;
    public static final int ACTION_TOGGLE_EXPLOSION_PROTECTION = 9;
    public static final int ACTION_DISBAND_SELF = 10;
    public static final int ACTION_ADD_ALLY = 11;
    public static final int ACTION_REMOVE_ALLY = 12;
    public static final int ACTION_ADD_ENEMY = 13;
    public static final int ACTION_REMOVE_ENEMY = 14;
    public static final int ACTION_TRANSFER_OWNERSHIP = 15;
    public static final int ACTION_SET_TRUST_LEVEL = 16;
    public static final int ACTION_SET_FAKEPLAYER_TRUST = 17;
    public static final int ACTION_SET_FREE_TO_JOIN = 18;
    public static final int ACTION_SET_COLOR = 19;
    public static final int ACTION_SET_TITLE = 20;
    public static final int ACTION_SET_DESCRIPTION = 21;
    public static final int ACTION_JOIN_FREE_PARTY = 22;

    private int action;
    private String stringArg;

    public MessagePartyAction() {}

    public MessagePartyAction(int action, String stringArg) {
        this.action = action;
        this.stringArg = stringArg;
    }

    public static MessagePartyAction create(String name) {
        return new MessagePartyAction(ACTION_CREATE, name);
    }

    public static MessagePartyAction disband() {
        return new MessagePartyAction(ACTION_DISBAND, "");
    }

    public static MessagePartyAction rename(String newName) {
        return new MessagePartyAction(ACTION_RENAME, newName);
    }

    public static MessagePartyAction invite(String username) {
        return new MessagePartyAction(ACTION_INVITE, username);
    }

    public static MessagePartyAction acceptInvite(int partyId) {
        return new MessagePartyAction(ACTION_ACCEPT_INVITE, String.valueOf(partyId));
    }

    public static MessagePartyAction kickOrLeave(String username) {
        return new MessagePartyAction(ACTION_KICK_OR_LEAVE, username);
    }

    public static MessagePartyAction changeRole(String usernameAndRole) {
        return new MessagePartyAction(ACTION_CHANGE_ROLE, usernameAndRole);
    }

    public static MessagePartyAction toggleBQuLink(boolean linked) {
        return new MessagePartyAction(ACTION_TOGGLE_BQU_LINK, linked ? "true" : "false");
    }

    public static MessagePartyAction toggleFakePlayers() {
        return new MessagePartyAction(ACTION_TOGGLE_FAKE_PLAYERS, "");
    }

    public static MessagePartyAction toggleExplosionProtection() {
        return new MessagePartyAction(ACTION_TOGGLE_EXPLOSION_PROTECTION, "");
    }

    public static MessagePartyAction disbandSelf() {
        return new MessagePartyAction(ACTION_DISBAND_SELF, "");
    }

    public static MessagePartyAction addAlly(String username) {
        return new MessagePartyAction(ACTION_ADD_ALLY, username);
    }

    public static MessagePartyAction removeAlly(String username) {
        return new MessagePartyAction(ACTION_REMOVE_ALLY, username);
    }

    public static MessagePartyAction addEnemy(String username) {
        return new MessagePartyAction(ACTION_ADD_ENEMY, username);
    }

    public static MessagePartyAction removeEnemy(String username) {
        return new MessagePartyAction(ACTION_REMOVE_ENEMY, username);
    }

    public static MessagePartyAction transferOwnership(String username) {
        return new MessagePartyAction(ACTION_TRANSFER_OWNERSHIP, username);
    }

    public static MessagePartyAction setTrustLevel(String actionAndLevel) {
        return new MessagePartyAction(ACTION_SET_TRUST_LEVEL, actionAndLevel);
    }

    public static MessagePartyAction setFakePlayerTrust(String level) {
        return new MessagePartyAction(ACTION_SET_FAKEPLAYER_TRUST, level);
    }

    public static MessagePartyAction setFreeToJoin(boolean free) {
        return new MessagePartyAction(ACTION_SET_FREE_TO_JOIN, free ? "true" : "false");
    }

    public static MessagePartyAction setColor(int color) {
        return new MessagePartyAction(ACTION_SET_COLOR, Integer.toString(color));
    }

    public static MessagePartyAction setTitle(String title) {
        return new MessagePartyAction(ACTION_SET_TITLE, title);
    }

    public static MessagePartyAction setDescription(String desc) {
        return new MessagePartyAction(ACTION_SET_DESCRIPTION, desc);
    }

    public static MessagePartyAction joinFreeParty(int partyId) {
        return new MessagePartyAction(ACTION_JOIN_FREE_PARTY, String.valueOf(partyId));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        stringArg = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        ByteBufUtils.writeUTF8String(buf, stringArg);
    }

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

    public static class Handler implements IMessageHandler<MessagePartyAction, IMessage> {

        @Override
        public IMessage onMessage(MessagePartyAction msg, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                IPartyProvider provider = PartyProviderRegistry.get();
                // When not BQu-linked, use self-managed provider to avoid
                // accidentally creating/modifying BQu parties
                boolean playerBQuLinked = PartyManagerData.getInstance()
                        .isBQuLinked(player.getUniqueID());
                DefaultPartyProvider selfProvider = new DefaultPartyProvider();
                IPartyProvider activeProvider = playerBQuLinked ? provider : selfProvider;

                String actionName = actionName(msg.action);

                boolean success = false;
                switch (msg.action) {
                    case ACTION_CREATE: {
                        String name = msg.stringArg.trim();
                        if (name.isEmpty()) name = "New Party";
                        success = selfProvider.createParty(player, name);
                        break;
                    }
                    case ACTION_DISBAND: {
                        // Ensure self-managed party exists (may need to create from BQu data)
                        Party ensured = getOrCreateSelfParty(player, provider);
                        PartyManagerData pmDisband = PartyManagerData.getInstance();
                        Party disbandParty = pmDisband.getPartyByPlayer(player.getUniqueID());
                        if (disbandParty != null) {
                            PartyRole disbandRole = disbandParty.getRole(player.getUniqueID());
                            if (disbandRole != PartyRole.OWNER && !player.canUseCommand(2, "")) {
                                break;
                            }
                        }
                        boolean disbanded = selfProvider.disbandParty(player);
                        pmDisband.setBQuLinked(player.getUniqueID(), false);
                        success = true;
                        break;
                    }
                    case ACTION_RENAME: {
                        String newName = msg.stringArg.trim();
                        if (!newName.isEmpty()) {
                            success = activeProvider.renameParty(player, newName);
                        }
                        break;
                    }
                    case ACTION_INVITE:
                        success = activeProvider.invitePlayer(player, msg.stringArg);
                        break;
                    case ACTION_ACCEPT_INVITE:
                        try {
                            int partyId = Integer.parseInt(msg.stringArg);
                            success = activeProvider.acceptInvite(player, partyId);
                        } catch (NumberFormatException ignored) {}
                        break;
                    case ACTION_KICK_OR_LEAVE:
                        success = activeProvider.kickOrLeave(player, msg.stringArg);
                        break;
                    case ACTION_CHANGE_ROLE: {
                        String[] parts = msg.stringArg.split(":", 2);
                        if (parts.length == 2) {
                            success = activeProvider.changeRole(player, parts[0], parts[1]);
                        }
                        break;
                    }
                    case ACTION_TOGGLE_BQU_LINK: {
                        boolean linked = "true".equals(msg.stringArg);
                        PartyManagerData pmLink = PartyManagerData.getInstance();
                        Party linkParty = pmLink.getPartyByPlayer(player.getUniqueID());
                        if (linkParty != null) {
                            PartyRole linkRole = linkParty.getRole(player.getUniqueID());
                            if (linkRole != null && !linkRole.canInvite() && !player.canUseCommand(2, "")) {
                                break;
                            }
                        }
                        if (linked) {
                            boolean hasBQuParty = provider.hasNativeParty(player.getUniqueID());
                            if (!hasBQuParty) {
                                break;
                            }
                            pmLink.setBQuLinked(player.getUniqueID(), true);
                        } else {
                            pmLink.setBQuLinked(player.getUniqueID(), false);
                            Party created = getOrCreateSelfParty(player, provider);
                        }
                        success = true;
                        break;
                    }
                    case ACTION_TOGGLE_FAKE_PLAYERS: {
                        // Legacy: cycle NONE -> ALLY -> MEMBER -> NONE
                        Party toggleParty = getOrCreateSelfParty(player, provider);
                        if (toggleParty != null) {
                            PartyRole toggleRole = toggleParty.getRole(player.getUniqueID());
                            if (toggleRole != null && toggleRole.canInvite()) {
                                TrustLevel cur = toggleParty.getFakePlayerTrustLevel();
                                TrustLevel next = cur == TrustLevel.NONE ? TrustLevel.ALLY :
                                        cur == TrustLevel.ALLY ? TrustLevel.MEMBER : TrustLevel.NONE;
                                toggleParty.setFakePlayerTrustLevel(next);
                                success = true;
                            }
                        }
                        break;
                    }
                    case ACTION_TOGGLE_EXPLOSION_PROTECTION: {
                        Party toggleParty = getOrCreateSelfParty(player, provider);
                        if (toggleParty != null) {
                            PartyRole toggleRole = toggleParty.getRole(player.getUniqueID());
                            if (toggleRole != null && toggleRole.canInvite()) {
                                toggleParty.setProtectExplosions(!toggleParty.protectsExplosions());
                                success = true;
                            }
                        }
                        break;
                    }
                    case ACTION_DISBAND_SELF:
                        break;
                    case ACTION_ADD_ALLY:
                    case ACTION_REMOVE_ALLY:
                    case ACTION_ADD_ENEMY:
                    case ACTION_REMOVE_ENEMY: {
                        Party allyParty = getOrCreateSelfParty(player, provider);
                        if (allyParty == null) break;
                        PartyRole allyRole = allyParty.getRole(player.getUniqueID());
                        if (allyRole == null || !allyRole.canInvite()) break;
                        net.minecraft.server.MinecraftServer srv = player.getServer();
                        if (srv == null) break;
                        net.minecraft.entity.player.EntityPlayerMP target = srv.getPlayerList()
                                .getPlayerByUsername(msg.stringArg);
                        if (target == null) break;
                        java.util.UUID targetId = target.getUniqueID();
                        if (allyParty.isMember(targetId)) break;
                        switch (msg.action) {
                            case ACTION_ADD_ALLY:
                                allyParty.addAlly(targetId);
                                break;
                            case ACTION_REMOVE_ALLY:
                                allyParty.removeAlly(targetId);
                                break;
                            case ACTION_ADD_ENEMY:
                                allyParty.addEnemy(targetId);
                                break;
                            case ACTION_REMOVE_ENEMY:
                                allyParty.removeEnemy(targetId);
                                break;
                        }
                        success = true;
                        break;
                    }
                    case ACTION_TRANSFER_OWNERSHIP: {
                        Party tParty = getOrCreateSelfParty(player, provider);
                        if (tParty == null) break;
                        PartyRole tRole = tParty.getRole(player.getUniqueID());
                        if (tRole != PartyRole.OWNER && !player.canUseCommand(2, "")) break;
                        net.minecraft.server.MinecraftServer srv = player.getServer();
                        if (srv == null) break;
                        net.minecraft.entity.player.EntityPlayerMP target = srv.getPlayerList()
                                .getPlayerByUsername(msg.stringArg);
                        if (target == null) break;
                        if (!tParty.isMember(target.getUniqueID())) break;
                        tParty.setRole(target.getUniqueID(), PartyRole.OWNER);
                        success = true;
                        break;
                    }
                    case ACTION_SET_TRUST_LEVEL: {
                        Party trustParty = getOrCreateSelfParty(player, provider);
                        if (trustParty == null) break;
                        PartyRole trustRole = trustParty.getRole(player.getUniqueID());
                        if (trustRole == null || !trustRole.canInvite()) break;
                        String[] tp = msg.stringArg.split(":", 2);
                        if (tp.length == 2) {
                            TrustAction ta = TrustAction.fromNbtKey(tp[0]);
                            TrustLevel tl = TrustLevel.fromName(tp[1]);
                            if (ta != null) {
                                trustParty.setTrustLevel(ta, tl);
                                success = true;
                            }
                        }
                        break;
                    }
                    case ACTION_SET_FAKEPLAYER_TRUST: {
                        Party fpParty = getOrCreateSelfParty(player, provider);
                        if (fpParty == null) break;
                        PartyRole fpRole = fpParty.getRole(player.getUniqueID());
                        if (fpRole == null || !fpRole.canInvite()) break;
                        fpParty.setFakePlayerTrustLevel(TrustLevel.fromName(msg.stringArg));
                        success = true;
                        break;
                    }
                    case ACTION_SET_FREE_TO_JOIN: {
                        Party fjParty = getOrCreateSelfParty(player, provider);
                        if (fjParty == null) break;
                        PartyRole fjRole = fjParty.getRole(player.getUniqueID());
                        if (fjRole == null || !fjRole.canInvite()) break;
                        fjParty.setFreeToJoin("true".equals(msg.stringArg));
                        success = true;
                        break;
                    }
                    case ACTION_SET_COLOR: {
                        Party cParty = getOrCreateSelfParty(player, provider);
                        if (cParty == null) break;
                        PartyRole cRole = cParty.getRole(player.getUniqueID());
                        if (cRole == null || !cRole.canInvite()) break;
                        try {
                            cParty.setColor(Integer.parseInt(msg.stringArg));
                        } catch (NumberFormatException ignored) {}
                        success = true;
                        break;
                    }
                    case ACTION_SET_TITLE: {
                        Party titleParty = getOrCreateSelfParty(player, provider);
                        if (titleParty == null) break;
                        PartyRole titleRole = titleParty.getRole(player.getUniqueID());
                        if (titleRole == null || !titleRole.canInvite()) break;
                        titleParty.setTitle(msg.stringArg.trim());
                        success = true;
                        break;
                    }
                    case ACTION_SET_DESCRIPTION: {
                        Party descParty = getOrCreateSelfParty(player, provider);
                        if (descParty == null) break;
                        PartyRole descRole = descParty.getRole(player.getUniqueID());
                        if (descRole == null || !descRole.canInvite()) break;
                        descParty.setDescription(msg.stringArg.trim());
                        success = true;
                        break;
                    }
                    case ACTION_JOIN_FREE_PARTY: {
                        try {
                            int joinId = Integer.parseInt(msg.stringArg);
                            PartyManagerData pmJoin = PartyManagerData.getInstance();
                            // Player must not already be in a party
                            if (pmJoin.getPartyByPlayer(player.getUniqueID()) != null) break;
                            Party joinParty = pmJoin.getParty(joinId);
                            if (joinParty == null || !joinParty.isFreeToJoin()) break;
                            joinParty.addMember(player.getUniqueID(), PartyRole.MEMBER);
                            success = true;
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                }

                if (success) {
                    provider.syncToAll();
                }
            });
            return null;
        }

        private static String actionName(int action) {
            switch (action) {
                case ACTION_CREATE:
                    return "CREATE";
                case ACTION_DISBAND:
                    return "DISBAND";
                case ACTION_RENAME:
                    return "RENAME";
                case ACTION_INVITE:
                    return "INVITE";
                case ACTION_ACCEPT_INVITE:
                    return "ACCEPT_INVITE";
                case ACTION_KICK_OR_LEAVE:
                    return "KICK_OR_LEAVE";
                case ACTION_CHANGE_ROLE:
                    return "CHANGE_ROLE";
                case ACTION_TOGGLE_BQU_LINK:
                    return "TOGGLE_BQU_LINK";
                case ACTION_TOGGLE_FAKE_PLAYERS:
                    return "TOGGLE_FAKE_PLAYERS";
                case ACTION_TOGGLE_EXPLOSION_PROTECTION:
                    return "TOGGLE_EXPLOSION";
                case ACTION_DISBAND_SELF:
                    return "DISBAND_SELF(deprecated)";
                case ACTION_ADD_ALLY:
                    return "ADD_ALLY";
                case ACTION_REMOVE_ALLY:
                    return "REMOVE_ALLY";
                case ACTION_ADD_ENEMY:
                    return "ADD_ENEMY";
                case ACTION_REMOVE_ENEMY:
                    return "REMOVE_ENEMY";
                case ACTION_TRANSFER_OWNERSHIP:
                    return "TRANSFER_OWNERSHIP";
                case ACTION_SET_TRUST_LEVEL:
                    return "SET_TRUST_LEVEL";
                case ACTION_SET_FAKEPLAYER_TRUST:
                    return "SET_FAKEPLAYER_TRUST";
                case ACTION_SET_FREE_TO_JOIN:
                    return "SET_FREE_TO_JOIN";
                case ACTION_SET_COLOR:
                    return "SET_COLOR";
                case ACTION_SET_TITLE:
                    return "SET_TITLE";
                case ACTION_SET_DESCRIPTION:
                    return "SET_DESCRIPTION";
                case ACTION_JOIN_FREE_PARTY:
                    return "JOIN_FREE_PARTY";
                default:
                    return "UNKNOWN(" + action + ")";
            }
        }
    }
}
