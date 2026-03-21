package com.github.gtexpert.teamclaim.integration.bqu;

import java.util.Collections;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;

import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.teamclaim.Tags;
import com.github.gtexpert.teamclaim.api.modules.TModule;
import com.github.gtexpert.teamclaim.api.party.PartyProviderRegistry;
import com.github.gtexpert.teamclaim.api.util.Mods;
import com.github.gtexpert.teamclaim.integration.IntegrationSubmodule;
import com.github.gtexpert.teamclaim.module.Modules;

@TModule(
         moduleID = Modules.MODULE_BQU,
         containerID = Tags.MODID,
         modDependencies = Mods.Names.BETTER_QUESTING,
         name = "TeamClaim BetterQuesting Integration",
         description = "BetterQuesting Integration Module. Uses BQu party system and migrates existing parties.")
public class BQuModule extends IntegrationSubmodule {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        PartyProviderRegistry.register(new BQPartyProvider());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            PartyProviderRegistry.registerNativeScreenOpener(BQuScreenHelper::openPartyScreen);
        }
    }

    @Override
    public void serverStarted(FMLServerStartedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            BQMigrationHelper.migrateIfNeeded(server.getEntityWorld());
        }
    }

    @NotNull
    @Override
    public List<Class<?>> getEventBusSubscribers() {
        return Collections.singletonList(BQPartyEventHandler.class);
    }
}
