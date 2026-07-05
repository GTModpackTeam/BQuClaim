package com.github.gtexpert.blpc.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.NotNull;

/** Base for the level-0 (any player) {@code /blpc <sub>} commands. */
public abstract class PlayerCommand extends CommandBase {

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(@NotNull MinecraftServer server, @NotNull ICommandSender sender) {
        return true;
    }
}
