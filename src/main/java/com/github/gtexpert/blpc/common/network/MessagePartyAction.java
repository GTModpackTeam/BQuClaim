package com.github.gtexpert.blpc.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.github.gtexpert.blpc.BLPCMod;
import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.party.DefaultPartyProvider;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;
import com.github.gtexpert.blpc.common.party.PartyRole;

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
                BLPCMod.LOGGER.debug("[PartyAction] player={} action={} arg={} bquLinked={}",
                        player.getName(), actionName, msg.stringArg, playerBQuLinked);

                boolean success = false;
                switch (msg.action) {
                    case ACTION_CREATE: {
                        String name = msg.stringArg.trim();
                        if (name.isEmpty()) name = "New Party";
                        success = selfProvider.createParty(player, name);
                        BLPCMod.LOGGER.debug("[PartyAction] CREATE result={}", success);
                        break;
                    }
                    case ACTION_DISBAND: {
                        // Ensure self-managed party exists (may need to create from BQu data)
                        Party ensured = getOrCreateSelfParty(player, provider);
                        BLPCMod.LOGGER.debug("[PartyAction] DISBAND ensured={}",
                                ensured != null ? ensured.getName() + "(id=" + ensured.getPartyId() + ")" : "null");
                        PartyManagerData pmDisband = PartyManagerData.getInstance();
                        Party disbandParty = pmDisband.getPartyByPlayer(player.getUniqueID());
                        if (disbandParty != null) {
                            PartyRole disbandRole = disbandParty.getRole(player.getUniqueID());
                            BLPCMod.LOGGER.debug("[PartyAction] DISBAND party={} role={} isOP={}",
                                    disbandParty.getName(), disbandRole, player.canUseCommand(2, ""));
                            if (disbandRole != PartyRole.OWNER && !player.canUseCommand(2, "")) {
                                BLPCMod.LOGGER.debug("[PartyAction] DISBAND DENIED: not owner");
                                break;
                            }
                        } else {
                            BLPCMod.LOGGER.debug("[PartyAction] DISBAND no self-managed party found");
                        }
                        boolean disbanded = selfProvider.disbandParty(player);
                        BLPCMod.LOGGER.debug("[PartyAction] DISBAND disbanded={}", disbanded);
                        pmDisband.setBQuLinked(player.getUniqueID(), false);
                        success = true;
                        break;
                    }
                    case ACTION_RENAME: {
                        String newName = msg.stringArg.trim();
                        if (!newName.isEmpty()) {
                            success = activeProvider.renameParty(player, newName);
                        }
                        BLPCMod.LOGGER.debug("[PartyAction] RENAME result={}", success);
                        break;
                    }
                    case ACTION_INVITE:
                        success = activeProvider.invitePlayer(player, msg.stringArg);
                        BLPCMod.LOGGER.debug("[PartyAction] INVITE target={} result={}", msg.stringArg, success);
                        break;
                    case ACTION_ACCEPT_INVITE:
                        try {
                            int partyId = Integer.parseInt(msg.stringArg);
                            success = activeProvider.acceptInvite(player, partyId);
                            BLPCMod.LOGGER.debug("[PartyAction] ACCEPT partyId={} result={}", partyId, success);
                        } catch (NumberFormatException ignored) {}
                        break;
                    case ACTION_KICK_OR_LEAVE:
                        success = activeProvider.kickOrLeave(player, msg.stringArg);
                        BLPCMod.LOGGER.debug("[PartyAction] KICK_OR_LEAVE target={} result={}",
                                msg.stringArg, success);
                        break;
                    case ACTION_CHANGE_ROLE: {
                        String[] parts = msg.stringArg.split(":", 2);
                        if (parts.length == 2) {
                            success = activeProvider.changeRole(player, parts[0], parts[1]);
                        }
                        BLPCMod.LOGGER.debug("[PartyAction] CHANGE_ROLE result={}", success);
                        break;
                    }
                    case ACTION_TOGGLE_BQU_LINK: {
                        boolean linked = "true".equals(msg.stringArg);
                        PartyManagerData pmLink = PartyManagerData.getInstance();
                        Party linkParty = pmLink.getPartyByPlayer(player.getUniqueID());
                        if (linkParty != null) {
                            PartyRole linkRole = linkParty.getRole(player.getUniqueID());
                            BLPCMod.LOGGER.debug("[PartyAction] LINK selfParty={} role={}",
                                    linkParty.getName(), linkRole);
                            if (linkRole != null && !linkRole.canInvite() && !player.canUseCommand(2, "")) {
                                BLPCMod.LOGGER.debug("[PartyAction] LINK DENIED: not admin+");
                                break;
                            }
                        } else {
                            BLPCMod.LOGGER.debug("[PartyAction] LINK no self-managed party");
                        }
                        if (linked) {
                            boolean hasBQuParty = provider.hasNativeParty(player.getUniqueID());
                            BLPCMod.LOGGER.debug("[PartyAction] LINK hasNativeParty={}", hasBQuParty);
                            if (!hasBQuParty) {
                                BLPCMod.LOGGER.debug("[PartyAction] LINK DENIED: no BQu party");
                                break;
                            }
                            pmLink.setBQuLinked(player.getUniqueID(), true);
                        } else {
                            pmLink.setBQuLinked(player.getUniqueID(), false);
                            Party created = getOrCreateSelfParty(player, provider);
                            BLPCMod.LOGGER.debug("[PartyAction] UNLINK ensuredSelfParty={}",
                                    created != null ? created.getName() : "null");
                        }
                        success = true;
                        BLPCMod.LOGGER.debug("[PartyAction] LINK/UNLINK result={}", success);
                        break;
                    }
                    case ACTION_TOGGLE_FAKE_PLAYERS: {
                        Party toggleParty = getOrCreateSelfParty(player, provider);
                        if (toggleParty != null) {
                            PartyRole toggleRole = toggleParty.getRole(player.getUniqueID());
                            if (toggleRole != null && toggleRole.canInvite()) {
                                toggleParty.setAllowFakePlayers(!toggleParty.allowsFakePlayers());
                                success = true;
                            }
                        }
                        BLPCMod.LOGGER.debug("[PartyAction] TOGGLE_FAKEPLAYERS result={}", success);
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
                        BLPCMod.LOGGER.debug("[PartyAction] TOGGLE_EXPLOSION result={}", success);
                        break;
                    }
                    case ACTION_DISBAND_SELF:
                        // Deprecated — same as ACTION_DISBAND
                        break;
                }

                BLPCMod.LOGGER.debug("[PartyAction] FINAL success={} -> syncToAll={}", success, success);
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
                default:
                    return "UNKNOWN(" + action + ")";
            }
        }
    }
}
