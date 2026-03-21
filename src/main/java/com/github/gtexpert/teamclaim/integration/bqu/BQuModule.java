package com.github.gtexpert.teamclaim.integration.bqu;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

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
         name = "TeamClaimMod BetterQuesting Integration",
         description = "BetterQuesting Integration Module. Enables party-based claim sharing.")
public class BQuModule extends IntegrationSubmodule {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        PartyProviderRegistry.register(new BQPartyProvider());
    }
}
