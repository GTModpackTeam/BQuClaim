package com.github.gtexpert.blpc.common.command;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.common.BLPCSaveHandler;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;

public class AcceptCommand extends CommandBase {

    @Override
    public @NotNull String getName() {
        return "accept";
    }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "/blpc accept <partyName>";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        String @NotNull [] args) throws CommandException {
        if (args.length != 1) {
            throw new CommandException("/blpc accept <partyName>");
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        Party party = BLPCCommandHelper.findPartyByName(args[0]);
        if (party == null) {
            throw new CommandException("Party not found: " + args[0]);
        }
        if (!party.hasInvite(player.getUniqueID())) {
            throw new CommandException("No pending invite from party: " + args[0]);
        }
        if (PartyManagerData.getInstance().getPartyByPlayer(player.getUniqueID()) != null) {
            throw new CommandException("You are already in a party. Leave first with /blpc leave.");
        }
        if (!party.canAddMember()) {
            throw new CommandException("Party is full.");
        }

        IPartyProvider provider = BLPCCommandHelper.activeProviderFor(player);
        boolean ok = provider.acceptInvite(player, party.getPartyId());
        if (!ok) {
            throw new CommandException("Failed to accept invite.");
        }
        provider.syncToAll();
        BLPCSaveHandler.INSTANCE.markDirty();
        sender.sendMessage(new TextComponentTranslation("command.blpc.accept.success", party.getName()));
    }

    @Override
    public @NotNull List<String> getTabCompletions(@NotNull MinecraftServer server,
                                                   @NotNull ICommandSender sender,
                                                   String @NotNull [] args, @Nullable BlockPos targetPos) {
        if (args.length == 1 && sender instanceof EntityPlayerMP player) {
            List<String> names = BLPCCommandHelper.pendingInvitesFor(player.getUniqueID()).stream()
                    .map(Party::getName)
                    .collect(Collectors.toList());
            return getListOfStringsMatchingLastWord(args, names);
        }
        return Collections.emptyList();
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
