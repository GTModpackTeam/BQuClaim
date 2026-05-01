package com.github.gtexpert.blpc.common.command;

import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.blpc.common.party.Party;

public class InvitesCommand extends CommandBase {

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
            UUID owner = party.getOwner();
            String ownerName = owner != null ? ListCommand.resolveName(server, party, owner) : "-";
            sender.sendMessage(new TextComponentString(String.format("- %s%s%s (%s)",
                    TextFormatting.AQUA, party.getName(), TextFormatting.RESET, ownerName)));
        }
        sender.sendMessage(new TextComponentTranslation("command.blpc.invites.hint"));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(@NotNull MinecraftServer server, @NotNull ICommandSender sender) {
        return true;
    }
}
