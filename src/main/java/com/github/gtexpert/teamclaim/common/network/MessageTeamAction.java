package com.github.gtexpert.teamclaim.common.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.github.gtexpert.teamclaim.api.party.IPartyProvider;
import com.github.gtexpert.teamclaim.api.party.PartyProviderRegistry;
import com.github.gtexpert.teamclaim.common.party.PartyManagerData;

import io.netty.buffer.ByteBuf;

public class MessageTeamAction implements IMessage {

    public static final int ACTION_CREATE = 0;
    public static final int ACTION_DISBAND = 1;
    public static final int ACTION_RENAME = 2;
    public static final int ACTION_INVITE = 3;
    public static final int ACTION_ACCEPT_INVITE = 4;
    public static final int ACTION_KICK_OR_LEAVE = 5;
    public static final int ACTION_CHANGE_ROLE = 6;
    public static final int ACTION_TOGGLE_BQU_LINK = 7;

    private static final long INVITE_DURATION_MS = 300000L; // 5 minutes

    private int action;
    private int partyId;
    private String stringArg;

    public MessageTeamAction() {}

    public MessageTeamAction(int action, int partyId, String stringArg) {
        this.action = action;
        this.partyId = partyId;
        this.stringArg = stringArg;
    }

    public static MessageTeamAction create(String name) {
        return new MessageTeamAction(ACTION_CREATE, -1, name);
    }

    public static MessageTeamAction disband(int partyId) {
        return new MessageTeamAction(ACTION_DISBAND, partyId, "");
    }

    public static MessageTeamAction rename(int partyId, String newName) {
        return new MessageTeamAction(ACTION_RENAME, partyId, newName);
    }

    public static MessageTeamAction invite(int partyId, String username) {
        return new MessageTeamAction(ACTION_INVITE, partyId, username);
    }

    public static MessageTeamAction acceptInvite(int partyId) {
        return new MessageTeamAction(ACTION_ACCEPT_INVITE, partyId, "");
    }

    public static MessageTeamAction kickOrLeave(int partyId, String username) {
        return new MessageTeamAction(ACTION_KICK_OR_LEAVE, partyId, username);
    }

    public static MessageTeamAction changeRole(int partyId, String usernameAndRole) {
        return new MessageTeamAction(ACTION_CHANGE_ROLE, partyId, usernameAndRole);
    }

    public static MessageTeamAction toggleBQuLink(boolean linked) {
        return new MessageTeamAction(ACTION_TOGGLE_BQU_LINK, -1, linked ? "true" : "false");
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readInt();
        partyId = buf.readInt();
        stringArg = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action);
        buf.writeInt(partyId);
        ByteBufUtils.writeUTF8String(buf, stringArg);
    }

    public static class Handler implements IMessageHandler<MessageTeamAction, IMessage> {

        @Override
        public IMessage onMessage(MessageTeamAction msg, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                IPartyProvider provider = PartyProviderRegistry.get();

                boolean success = false;
                switch (msg.action) {
                    case ACTION_CREATE:
                        String name = msg.stringArg.trim();
                        if (name.isEmpty()) name = "New Party";
                        success = provider.createParty(player, name);
                        break;
                    case ACTION_DISBAND:
                        success = provider.disbandParty(player, msg.partyId);
                        break;
                    case ACTION_RENAME:
                        String newName = msg.stringArg.trim();
                        if (!newName.isEmpty()) {
                            success = provider.renameParty(player, msg.partyId, newName);
                        }
                        break;
                    case ACTION_INVITE:
                        success = provider.invitePlayer(player, msg.partyId, msg.stringArg);
                        break;
                    case ACTION_ACCEPT_INVITE:
                        success = provider.acceptInvite(player, msg.partyId);
                        break;
                    case ACTION_KICK_OR_LEAVE:
                        success = provider.kickOrLeave(player, msg.partyId, msg.stringArg);
                        break;
                    case ACTION_CHANGE_ROLE:
                        String[] parts = msg.stringArg.split(":", 2);
                        if (parts.length == 2) {
                            success = provider.changeRole(player, msg.partyId, parts[0], parts[1]);
                        }
                        break;
                    case ACTION_TOGGLE_BQU_LINK:
                        boolean linked = "true".equals(msg.stringArg);
                        PartyManagerData.getInstance().setBQuLinked(player.getUniqueID(), linked);
                        success = true;
                        break;
                }

                if (success) {
                    provider.syncToAll();
                }
            });
            return null;
        }
    }
}
