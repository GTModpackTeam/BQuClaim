package com.github.gtexpert.teamclaim.core;

import java.util.Collections;
import java.util.List;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.teamclaim.Tags;
import com.github.gtexpert.teamclaim.TeamClaimMod;
import com.github.gtexpert.teamclaim.api.modules.IModule;
import com.github.gtexpert.teamclaim.api.modules.TModule;
import com.github.gtexpert.teamclaim.api.party.PartyProviderRegistry;
import com.github.gtexpert.teamclaim.common.TeamClaimSaveHandler;
import com.github.gtexpert.teamclaim.common.chunk.TicketManager;
import com.github.gtexpert.teamclaim.common.network.ModNetwork;
import com.github.gtexpert.teamclaim.common.party.DefaultPartyProvider;
import com.github.gtexpert.teamclaim.module.Modules;

@TModule(
         moduleID = Modules.MODULE_CORE,
         containerID = Tags.MODID,
         name = "TeamClaim Core",
         description = "Core module of TeamClaim",
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
        PartyProviderRegistry.register(new DefaultPartyProvider());
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        TeamClaimSaveHandler.INSTANCE.loadAll(event.getServer());
    }

    @Override
    public void serverStopping(FMLServerStoppingEvent event) {
        TeamClaimSaveHandler.INSTANCE.saveAll();
    }

    @NotNull
    @Override
    public List<Class<?>> getEventBusSubscribers() {
        return Collections.singletonList(CoreEventHandler.class);
    }
}
