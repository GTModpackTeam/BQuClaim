package com.github.gtexpert.blpc.common.command;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.blpc.api.party.Party;

public class InvitesCommand extends PlayerCommand {

    @Override
    public @NotNull String getName() {
        return "invites";
    }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "/blpc invites";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        String @NotNull [] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        List<Party> invites = BLPCCommandHelper.pendingInvitesFor(player.getUniqueID());
        if (invites.isEmpty()) {
            sender.sendMessage(new TextComponentTranslation("command.blpc.invites.empty"));
            return;
        }
        sender.sendMessage(new TextComponentTranslation("command.blpc.invites.header", invites.size()));
        for (Party party : invites) {
            String ownerName = BLPCCommandHelper.resolveOwnerName(server, party);
            sender.sendMessage(new TextComponentString(String.format("- %s%s%s (%s)",
                    TextFormatting.AQUA, party.getName(), TextFormatting.RESET, ownerName)));
        }
        sender.sendMessage(new TextComponentTranslation("command.blpc.invites.hint"));
    }
}
