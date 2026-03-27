package com.github.gtexpert.blpc.integration;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.gtexpert.blpc.Tags;
import com.github.gtexpert.blpc.api.modules.TModule;
import com.github.gtexpert.blpc.common.ModLog;
import com.github.gtexpert.blpc.module.BaseModule;
import com.github.gtexpert.blpc.module.Modules;

@TModule(
         moduleID = Modules.MODULE_INTEGRATION,
         containerID = Tags.MODID,
         name = "BLPCMod Mod Integration",
         description = "General BLPCMod Integration Module. Disabling this disables all integration modules.")
public class IntegrationModule extends BaseModule {

    @NotNull
    @Override
    public Logger getLogger() {
        return ModLog.ROOT;
    }
}
