package com.github.gtexpert.teamclaim.core;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.teamclaim.Tags;
import com.github.gtexpert.teamclaim.TeamClaimMod;
import com.github.gtexpert.teamclaim.api.modules.IModule;
import com.github.gtexpert.teamclaim.api.modules.TModule;
import com.github.gtexpert.teamclaim.common.chunk.TicketManager;
import com.github.gtexpert.teamclaim.common.network.ModNetwork;
import com.github.gtexpert.teamclaim.module.Modules;

@TModule(
         moduleID = Modules.MODULE_CORE,
         containerID = Tags.MODID,
         name = "TeamClaimMod Core",
         description = "Core module of TeamClaimMod",
         coreModule = true)
public class CoreModule implements IModule {

    public static final Logger logger = LogManager.getLogger(Tags.MODNAME + " Core");

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public void registerPackets() {
        ModNetwork.init();
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ForgeChunkManager.setForcedChunkLoadingCallback(TeamClaimMod.INSTANCE, new TicketManager());
    }
}
