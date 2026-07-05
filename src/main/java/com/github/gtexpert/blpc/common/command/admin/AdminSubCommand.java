package com.github.gtexpert.blpc.common.command.admin;

import net.minecraft.command.CommandBase;

/** Base for the level-3 (op-only) {@code /blpc admin <sub>} commands. */
public abstract class AdminSubCommand extends CommandBase {

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }
}
