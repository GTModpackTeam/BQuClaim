package com.github.gtexpert.blpc.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.blpc.api.party.IPartyProvider;
import com.github.gtexpert.blpc.common.BLPCSaveHandler;
import com.github.gtexpert.blpc.common.party.Party;
import com.github.gtexpert.blpc.common.party.PartyManagerData;
import com.github.gtexpert.blpc.common.party.PartyRole;

public class LeaveCommand extends CommandBase {

    @Override
    public @NotNull String getName() {
        return "leave";
    }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "/blpc leave";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        String @NotNull [] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        Party party = PartyManagerData.getInstance().getPartyByPlayer(player.getUniqueID());
        if (party == null) {
            throw new CommandException("You are not in a party.");
        }
        if (party.getRole(player.getUniqueID()) == PartyRole.OWNER && party.getMembers().size() > 1) {
            throw new CommandException(
                    "Owners cannot leave while members remain. Transfer ownership first with /blpc admin move-owner, or /blpc admin disband.");
        }

        String partyName = party.getName();
        IPartyProvider provider = BLPCCommandHelper.activeProviderFor(player);
        boolean ok = provider.kickOrLeave(player, player.getName());
        if (!ok) {
            throw new CommandException("Failed to leave party.");
        }
        PartyManagerData.getInstance().setBQuLinked(player.getUniqueID(), false);
        provider.syncToAll();
        BLPCSaveHandler.INSTANCE.markDirty();
        sender.sendMessage(new TextComponentTranslation("command.blpc.leave.success", partyName));
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
