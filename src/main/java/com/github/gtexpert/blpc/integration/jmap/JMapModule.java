package com.github.gtexpert.blpc.integration.jmap;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.api.modules.TModule;
import com.github.gtexpert.blpc.api.util.Mods;
import com.github.gtexpert.blpc.integration.IntegrationSubmodule;
import com.github.gtexpert.blpc.module.Modules;

@TModule(
         moduleID = Modules.MODULE_JMAP,
         containerID = Tags.MODID,
         modDependencies = Mods.Names.JOURNEY_MAP,
         name = "BLPC JourneyMap Integration",
         description = "JourneyMap Integration Module. Displays chunk claim overlays on JourneyMap.")
public class JMapModule extends IntegrationSubmodule {

    @NotNull
    @Override
    public List<Class<?>> getEventBusSubscribers() {
        return Collections.singletonList(JMapClaimSyncHandler.class);
    }
}
