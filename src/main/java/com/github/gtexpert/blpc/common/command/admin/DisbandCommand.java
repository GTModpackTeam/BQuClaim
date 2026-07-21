package com.github.gtexpert.blpc.common.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.gtexpert.blpc.api.party.Party;
import com.github.gtexpert.blpc.api.party.PartyProviderRegistry;
import com.github.gtexpert.blpc.common.BLPCSaveHandler;
import com.github.gtexpert.blpc.common.chunk.ChunkManagerData;
import com.github.gtexpert.blpc.common.command.BLPCCommandHelper;
import com.github.gtexpert.blpc.common.network.ModNetwork;
import com.github.gtexpert.blpc.common.network.message.ClientNotify;
import com.github.gtexpert.blpc.common.party.PartyManagerData;

public class DisbandCommand extends AdminSubCommand {

    @Override
    public @NotNull String getName() {
        return "disband";
    }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "/blpc admin disband <partyName>";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        String @NotNull [] args) throws CommandException {
        if (args.length != 1) {
            throw new CommandException("/blpc admin disband <partyName>");
        }
        Party party = BLPCCommandHelper.requirePartyByName(args[0]);

        String partyName = party.getName();
        List<UUID> members = new ArrayList<>(party.getMemberUUIDs());

        PartyManagerData pm = PartyManagerData.getInstance();
        pm.removeParty(party.getPartyId());
        ChunkManagerData.getInstance().releaseAllMemberClaims(members, sender.getEntityWorld());
        for (UUID memberId : members) {
            pm.setBQuLinked(memberId, false);
        }
        PartyProviderRegistry.get().syncToAll();
        BLPCSaveHandler.INSTANCE.markDirty();

        ModNetwork.broadcastToMembers(members, null, server,
                ClientNotify.partyEvent(ClientNotify.EVENT_DISBANDED, "", ""));
        sender.sendMessage(new TextComponentTranslation("command.blpc.disband.success", partyName));
    }

    @Override
    public @NotNull List<String> getTabCompletions(@NotNull MinecraftServer server,
                                                   @NotNull ICommandSender sender,
                                                   String @NotNull [] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, BLPCCommandHelper.allPartyNames());
        }
        return Collections.emptyList();
    }
}
